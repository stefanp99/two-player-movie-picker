package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.exceptions.MovieNotFoundException;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbDiscover;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.core.IdElement;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
import info.movito.themoviedbapi.model.core.video.Video;
import info.movito.themoviedbapi.model.core.video.VideoResults;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.builders.discover.DiscoverMovieParamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.DISCOVER_SORT_BY;
import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.MAX_DISCOVER_PAGE;
import static com.andreea.twoplayermoviepicker.utils.ConfigVariables.MOVIES_FETCHED_FROM_DISCOVER;
import static com.andreea.twoplayermoviepicker.utils.Constants.MAX_NUMBER_BASE_36;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_DISCOVER_PAGE_SIZE;
import static com.andreea.twoplayermoviepicker.utils.Constants.YOUTUBE_VIDEO_BASE_URL;
import static com.andreea.twoplayermoviepicker.utils.UtilityMethods.isSeedValid;
import static java.lang.String.format;

@Slf4j
@Service
public class TmdbService {
    private final TmdbApi tmdbApi;

    public TmdbService(@Value("${tmdb-api-key}") String tmdbApiKey) {
        tmdbApi = new TmdbApi(tmdbApiKey);
    }

    /**
     * Retrieves a random list of movies from the discover endpoint using a specified language and seed.
     *
     * @param language the language in which the movie data should be retrieved
     * @param seed     a string seed used to generate randomization for selecting movies
     * @return a ResponseEntity containing a list of movie responses or an error response in case of invalid seed,
     * no movies found, or other issues
     */
    public ResponseEntity<List<MovieResponse>> getRandomMoviesFromDiscover(String language,
                                                                           String seed) {
        List<MovieResponse> movieResponseList;

        if (!isSeedValid(seed)) {
            log.warn("Invalid seed received in getRandomMoviesFromDiscover: {}", seed);
            return ResponseEntity.badRequest().build();
        }
        long seedLong = Long.parseLong(seed.toUpperCase(), 36);
        Random random = new Random(seedLong);
        Integer discoverPage = random.nextInt(MAX_DISCOVER_PAGE) + 1;

        log.info("Fetching discover movies for seed {} (page {}), language {}", seed, discoverPage, language);
        MovieResultsPage movieResultsPage = getMovieResultsPageFromDiscover(discoverPage, language);
        if (movieResultsPage != null) {
            movieResponseList = getMovieResponseListFromMovieResultsPage(movieResultsPage, language, random);
            log.info("Successfully fetched {} movies from discover", movieResponseList.size());
            return ResponseEntity.ok(movieResponseList);
        }
        return ResponseEntity.notFound().build();
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
    public ResponseEntity<String> getYoutubeTrailer(Integer movieId, String language) {
        try {
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
        } catch (TmdbException e) {
            log.error("Error fetching YouTube trailer for movie {}: {}", movieId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private MovieDb getMovieById(Integer id, String language) {
        try {
            TmdbMovies tmdbMovies = tmdbApi.getMovies();
            return tmdbMovies.getDetails(id, language);
        } catch (TmdbException e) {
            log.error("Failed to fetch movie details for ID {}: {}", id, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private MovieResultsPage getMovieResultsPageFromDiscover(Integer page, String language) {
        try {
            TmdbDiscover tmdbDiscover = tmdbApi.getDiscover();
            DiscoverMovieParamBuilder discoverMovieParamBuilder = new DiscoverMovieParamBuilder()
                    .includeAdult(false)
                    .includeVideo(false)
                    .language(language)
                    .page(page)
                    .sortBy(DISCOVER_SORT_BY);
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

        List<Integer> randomNumbers = getRandomNumbersFromEnumeration(random);

        List<MovieResponse> movieResponseList = new ArrayList<>();
        for (Integer index : randomNumbers) {
            MovieDb movieDb = getMovieById(movieIdList.get(index), language);
            movieResponseList.add(MovieResponse.fromMovie(movieDb));
        }
        return movieResponseList;
    }

    private List<Integer> getRandomNumbersFromEnumeration(Random random) {
        List<Integer> randomNumbers = new ArrayList<>();

        for (int i = 0; i < MOVIES_FETCHED_FROM_DISCOVER; i++) {
            int randomNumber = random.nextInt(TMDB_DISCOVER_PAGE_SIZE - 1);
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
