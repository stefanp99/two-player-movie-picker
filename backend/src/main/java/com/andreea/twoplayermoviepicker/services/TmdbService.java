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

    public ResponseEntity<List<MovieResponse>> getRandomMoviesFromDiscover(String language,
                                                                           String seed) {//TODO: add nr of movies param
        List<MovieResponse> movieResponseList;

        if (!isSeedValid(seed)) {
            return ResponseEntity.badRequest().build();
        }

        long seedLong = Long.parseLong(seed.toUpperCase(), 36);

        Random random = new Random(seedLong);

        Integer discoverPage = random.nextInt(MAX_DISCOVER_PAGE) + 1;

        MovieResultsPage movieResultsPage = getMovieResultsPageFromDiscover(discoverPage, language);
        if (movieResultsPage != null) {
            movieResponseList = getMovieResponseListFromMovieResultsPage(movieResultsPage, language, random);
        } else {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(movieResponseList);
    }

    public String generateSeed(String oldSeed) {
        if (!oldSeed.matches("^[a-zA-Z0-9]{4}$")) {
            return null;
        }

        long oldSeedLong = Long.parseLong(oldSeed.toUpperCase(), 36);
        Random random = new Random(oldSeedLong);
        long nextSeedLong = random.nextLong(MAX_NUMBER_BASE_36);
        String newSeed = Long.toString(nextSeedLong, 36).toUpperCase();
        while (newSeed.length() < 4) {
            newSeed = "0".concat(newSeed);
        }
        return newSeed;
    }

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
                return ResponseEntity.ok(YOUTUBE_VIDEO_BASE_URL + trailer.getKey());
            }
            return ResponseEntity.notFound().build();
        } catch (TmdbException e) {
            throw new RuntimeException(e);
        }
    }

    private MovieDb getMovieById(Integer id, String language) {
        try {
            TmdbMovies tmdbMovies = tmdbApi.getMovies();
            return tmdbMovies.getDetails(id, language);
        } catch (TmdbException e) {
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
