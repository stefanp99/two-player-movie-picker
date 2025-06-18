package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.exceptions.MovieNotFoundException;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbDiscover;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.core.IdElement;
import info.movito.themoviedbapi.model.core.MovieResultsPage;
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
import static com.andreea.twoplayermoviepicker.utils.Constants.MAX_NUMBER_BASE_36;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_DISCOVER_PAGE_SIZE;
import static java.lang.String.format;

@Slf4j
@Service
public class TmdbService {
    private final TmdbApi tmdbApi;

    public TmdbService(@Value("${tmdb-api-key}") String tmdbApiKey) {
        tmdbApi = new TmdbApi(tmdbApiKey);
    }

    public ResponseEntity<List<MovieResponse>> getRandomMoviesFromDiscover(String language,
                                                                           Integer limit,
                                                                           String seed) {
        List<MovieResponse> movieResponseList;

        if (!seed.matches("^[a-zA-Z0-9]{4}$")) {
            return ResponseEntity.badRequest().build();
        }

        long seedLong = Long.parseLong(seed.toUpperCase(), 36);

        Random random = new Random(seedLong);

        Integer discoverPage = random.nextInt(MAX_DISCOVER_PAGE) + 1;

        MovieResultsPage movieResultsPage = getMovieResultsPageFromDiscover(discoverPage, language);
        if (movieResultsPage != null) {
            movieResponseList = getMovieResponseListFromMovieResultsPage(movieResultsPage, language, limit, random);
        } else {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(movieResponseList);
    }

    public ResponseEntity<String> generateSeed(String oldSeed) {
        if (!oldSeed.matches("^[a-zA-Z0-9]{4}$")) {
            return ResponseEntity.badRequest().build();
        }

        long oldSeedLong = Long.parseLong(oldSeed.toUpperCase(), 36);
        Random random = new Random(oldSeedLong);
        long nextSeedLong = random.nextLong(MAX_NUMBER_BASE_36);
        String newSeed = Long.toString(nextSeedLong, 36).toUpperCase();
        while (newSeed.length() < 4) {
            newSeed = "0".concat(newSeed);
        }
        return ResponseEntity.ok(newSeed);
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
                                                                         Integer limit,
                                                                         Random random) {
        if (movieResultsPage.getResults() == null) {
            throw new MovieNotFoundException(format("Movies not found on discover page %s", movieResultsPage.getId()));
        }
        List<Integer> movieIdList = movieResultsPage.getResults().stream()
                .map(IdElement::getId)
                .toList();

        List<Integer> randomNumbers = getRandomNumbersFromEnumeration(limit, random);

        List<MovieResponse> movieResponseList = new ArrayList<>();
        for (Integer index : randomNumbers) {
            MovieDb movieDb = getMovieById(movieIdList.get(index), language);
            movieResponseList.add(MovieResponse.fromMovie(movieDb));
        }
        return movieResponseList;
    }

    private List<Integer> getRandomNumbersFromEnumeration(Integer limit, Random random) {
        List<Integer> randomNumbers = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            int randomNumber = random.nextInt(TMDB_DISCOVER_PAGE_SIZE - 1);
            if (randomNumbers.contains(randomNumber)) {
                i--;
            } else {
                randomNumbers.add(randomNumber);
            }
        }

        return randomNumbers;
    }
}
