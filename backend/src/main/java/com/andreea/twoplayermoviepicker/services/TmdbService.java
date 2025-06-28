package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.exceptions.MovieNotFoundException;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.response_models.GenreResponse;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import com.andreea.twoplayermoviepicker.response_models.ProviderResponse;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbDiscover;
import info.movito.themoviedbapi.model.core.IdElement;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.core.video.Video;
import info.movito.themoviedbapi.model.core.video.VideoResults;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.model.watchproviders.Provider;
import info.movito.themoviedbapi.model.watchproviders.ProviderResults;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.DISCOVER_SORT_BY;
import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.MAX_DISCOVER_PAGE;
import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.MOVIES_FETCHED_FROM_DISCOVER;
import static com.andreea.twoplayermoviepicker.utils.Constants.EN_LANGUAGE_CODE;
import static com.andreea.twoplayermoviepicker.utils.Constants.MAX_NUMBER_BASE_36;
import static com.andreea.twoplayermoviepicker.utils.Constants.POPULAR_WATCH_PROVIDER_NAMES;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_DISCOVER_PAGE_SIZE;
import static com.andreea.twoplayermoviepicker.utils.Constants.YOUTUBE_VIDEO_BASE_URL;
import static java.lang.String.format;

@Slf4j
@Service
public class TmdbService {
    private final TmdbApi tmdbApi;

    public TmdbService(@Value("${tmdb-api-key}") String tmdbApiKey) {
        tmdbApi = new TmdbApi(tmdbApiKey);
    }

    /**
     * Retrieves a random list of movies from the discover endpoint based on the provided request parameters.
     * The randomness is determined using a seed value from the request, which ensures consistent results for the same seed.
     *
     * @param request the request containing parameters such as seed, language, genres, watch region, and watch providers.
     *                - seed: A string used to generate a random seed for fetching movies.
     *                - language: The language in which the movies' information should be fetched.
     *                - genres: A list of genre IDs to filter the movies.
     *                - watchRegion: The region code used to filter movies based on availability.
     *                - watchProviders: A list of provider IDs to filter the movies.
     * @return a list of movies that match the given parameters, or an empty list if no movies could be retrieved.
     */
    public List<MovieResponse> getRandomMoviesFromDiscover(RoomRequest request) {
        String seed = request.seed();
        String language = request.language();
        List<Integer> genres = request.genres();
        String watchRegion = request.watchRegion();
        List<Integer> watchProviders = request.watchProviders();

        long seedLong = Long.parseLong(seed.toUpperCase(), 36);
        Random random = new Random(seedLong);
        int discoverPage = random.nextInt(MAX_DISCOVER_PAGE) + 1;

        log.info("Fetching discover movies for seed {} (page {}), language {}, genres {}, watchRegion {}, watchProviders {}",
                seed, discoverPage, language, genres, watchRegion, watchProviders);

        MovieResultsPage movieResultsPage = getMovieResultsPageFromDiscover(
                discoverPage, language, genres, watchRegion, watchProviders);

        if (movieResultsPage.getTotalPages() == 0) {
            log.info("Failed to fetch movies from discover for seed {} due to no results", seed);
            return Collections.emptyList();
        }

        if (movieResultsPage.getTotalPages() < discoverPage) {
            log.info("Failed to fetch movies from discover for seed {} (page {}) due to page limit", seed, discoverPage);
            discoverPage = random.nextInt(movieResultsPage.getTotalPages()) + 1;
            log.info("Retrying with page {}...", discoverPage);
            movieResultsPage = getMovieResultsPageFromDiscover(
                    discoverPage, language, genres, watchRegion, watchProviders);
        }

        if (movieResultsPage != null) {
            List<MovieResponse> movieResponseList = getMovieResponseListFromMovieResultsPage(
                    movieResultsPage, language, random
            );
            log.info("Successfully fetched {} movies from discover", movieResponseList.size());
            return movieResponseList;
        }

        log.warn("Failed to fetch movies from discover for seed {} (page {})", seed, discoverPage);
        return Collections.emptyList();
    }


    /**
     * Generates a new random seed based on the provided old seed. The old seed must be a valid
     * alphanumeric string of exactly 4 characters. The new seed is also represented as a
     * 4-character alphanumeric string.
     *
     * @param oldSeed the previous seed to base the new seed on, must match the format "[a-zA-Z0-9]{4}"
     * @return the newly generated 4-character alphanumeric seed, or null if the input seed is invalid
     */
    public String generateSeed(String oldSeed) {
        if (!oldSeed.matches("^[a-zA-Z0-9]{4}$")) {
            log.warn("Invalid old seed received for generation: {}", oldSeed);
            return null;
        }

        long oldSeedLong = Long.parseLong(oldSeed.toUpperCase(), 36);
        Random random = new Random(oldSeedLong);
        long nextSeedLong = random.nextLong(MAX_NUMBER_BASE_36);
        String newSeed = Long.toString(nextSeedLong, 36).toUpperCase();
        while (newSeed.length() < 4) {
            newSeed = "0".concat(newSeed);
        }

        log.info("Generated new seed {} from old seed {}", newSeed, oldSeed);
        return newSeed;
    }

    /**
     * Retrieves the YouTube trailer URL for a specific movie based on its ID and language preferences.
     * The method searches for the most suitable video marked as a "Trailer" or "Featurette" and hosted on YouTube.
     * If no relevant video is found, a 404 response is returned. In case of TMDB API errors, an exception is thrown.
     *
     * @param movieId  the ID of the movie for which the trailer is to be retrieved
     * @param language the language code (e.g., "en", "fr") indicating the preferred language for the trailer
     * @return a ResponseEntity containing the YouTube trailer URL as a string if found,
     * or a 404 Not Found response if no suitable video is available
     */
    public ResponseEntity<String> getYoutubeTrailer(Integer movieId, String language) throws TmdbException {
        VideoResults videoResults = tmdbApi.getMovies().getVideos(movieId, language);
        List<Video> videos = videoResults.getResults();

        Video trailer = findBestVideo(videos, "Trailer", true);

        if (trailer == null) {
            trailer = findBestVideo(videos, "Trailer", false);
        }
        if (trailer == null) {
            trailer = findBestVideo(videos, "Featurette", false);
        }
        if (trailer != null) {
            String trailerUrl = YOUTUBE_VIDEO_BASE_URL + trailer.getKey();
            log.info("Found YouTube trailer for movie {}: {}", movieId, trailerUrl);
            return ResponseEntity.ok(trailerUrl);
        }
        log.warn("No suitable YouTube trailer found for movie {}", movieId);
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves a list of popular watch providers for a given region and language.
     *
     * @param watchRegion the region for which to retrieve watch providers (e.g., "US", "UK").
     * @param language    the language in which the watch provider information should be retrieved (e.g., "en-US").
     * @return a ResponseEntity containing a list of ProviderResponse objects, representing the watch providers.
     * @throws TmdbException if an error occurs while fetching watch provider data from the TMDb API.
     */
    public ResponseEntity<List<ProviderResponse>> getWatchProviders(String watchRegion, String language) throws TmdbException {
        ProviderResults providerResults = tmdbApi.getWatchProviders()
                .getMovieProviders(language, watchRegion);
        List<ProviderResponse> providerResponses = providerResults.getResults().stream()
                .sorted(Comparator.comparingInt(Provider::getDisplayPriority))
                .filter(provider -> POPULAR_WATCH_PROVIDER_NAMES.contains(provider.getProviderName()))
                .map(ProviderResponse::fromProvider)
                .toList();
        log.info("Successfully retrieved {} watch providers for region {} and language {}",
                providerResponses.size(), watchRegion, language);
        return ResponseEntity.ok(providerResponses);
    }

    /**
     * Retrieves a list of genres from the TMDB API and maps them to GenreResponse objects.
     *
     * @return a ResponseEntity containing a list of GenreResponse objects representing movie genres
     * @throws TmdbException if there is an error while fetching genres from the TMDB API
     */
    public ResponseEntity<List<GenreResponse>> getGenresResponse() throws TmdbException {
        List<GenreResponse> genreResponses = tmdbApi.getGenre().getMovieList(EN_LANGUAGE_CODE).stream()
                .map(GenreResponse::fromGenre)
                .toList();
        log.info("Successfully retrieved {} genres", genreResponses.size());
        return ResponseEntity.ok(genreResponses);
    }

    private MovieDb getMovieById(Integer id, String language) {
        try {
            return tmdbApi.getMovies().getDetails(id, language);
        } catch (TmdbException e) {
            log.error("Failed to fetch movie details for ID {}: {}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private MovieResultsPage getMovieResultsPageFromDiscover(Integer page, String language,
                                                             List<Integer> genres, String watchRegion,
                                                             List<Integer> watchProviders) {
        try {
            TmdbDiscover tmdbDiscover = tmdbApi.getDiscover();
            DiscoverMovieParamBuilder discoverMovieParamBuilder = new DiscoverMovieParamBuilder()
                    .includeAdult(false)
                    .includeVideo(false)
                    .language(language)
                    .page(page)
                    .sortBy(DISCOVER_SORT_BY);
            if (genres != null && !genres.isEmpty()) {
                discoverMovieParamBuilder.withGenres(genres, true);
            }
            if (watchRegion != null && !watchRegion.isBlank()) {
                discoverMovieParamBuilder.watchRegion(watchRegion);
            }
            if (watchProviders != null && !watchProviders.isEmpty()) {
                discoverMovieParamBuilder.withWatchProviders(watchProviders, true);
            }
            return tmdbDiscover.getMovie(discoverMovieParamBuilder);
        } catch (TmdbException e) {
            log.error("Failed to retrieve movies from discover page {}: {}", page, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private List<MovieResponse> getMovieResponseListFromMovieResultsPage(MovieResultsPage movieResultsPage,
                                                                         String language,
                                                                         Random random) {
        if (movieResultsPage.getResults() == null) {
            throw new MovieNotFoundException(format("Movies not found on discover page %s", movieResultsPage.getId()));
        }
        List<Integer> movieIdList = movieResultsPage.getResults().stream()
                .map(IdElement::getId)
                .toList();

        List<Integer> randomNumbers = getRandomNumbersFromEnumeration(random, movieResultsPage.getResults().size());

        List<MovieResponse> movieResponseList = new ArrayList<>();
        for (Integer index : randomNumbers) {
            MovieDb movieDb = getMovieById(movieIdList.get(index), language);
            movieResponseList.add(MovieResponse.fromMovie(movieDb));
        }
        return movieResponseList;
    }

    private List<Integer> getRandomNumbersFromEnumeration(Random random, Integer movieResultsPageSize) {
        List<Integer> randomNumbers = new ArrayList<>();
        int pageSize = TMDB_DISCOVER_PAGE_SIZE;

        if (movieResultsPageSize < TMDB_DISCOVER_PAGE_SIZE) {
            pageSize = movieResultsPageSize;
        }

        for (int i = 0; i < MOVIES_FETCHED_FROM_DISCOVER; i++) {
            int randomNumber = random.nextInt(pageSize);
            if (randomNumbers.contains(randomNumber)) {
                i--;
            } else {
                randomNumbers.add(randomNumber);
            }
        }

        return randomNumbers;
    }

    private Video findBestVideo(List<Video> videos, String type, boolean requireOfficial) {
        return videos.stream()
                .sorted((v1, v2) -> v2.getSize().compareTo(v1.getSize()))
                .filter(video -> video.getSite().equals("YouTube") &&
                        video.getType().equals(type) &&
                        (!requireOfficial || video.getOfficial()))
                .findFirst()
                .orElse(null);
    }
}
