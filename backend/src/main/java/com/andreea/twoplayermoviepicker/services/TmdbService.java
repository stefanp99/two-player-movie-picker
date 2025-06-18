package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.exceptions.MovieNotFoundException;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.MovieResultsPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
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
    private final Tmdb tmdb;

    public TmdbService(@Value("${tmdb-api-key}") String tmdbApiKey) {
        tmdb = new Tmdb(tmdbApiKey);
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

    private Movie getMovieById(Integer id, String language) {
        Response<Movie> movieResponse;
        try {
            movieResponse = tmdb.moviesService()
                    .summary(id, language)
                    .execute();

            if (movieResponse.isSuccessful()) {
                return movieResponse.body();
            }
            log.warn("Movie not found by id: {}", id);
        } catch (IOException e) {
            //TODO: see execute() javadoc
            log.warn("IOException while getting movie by id: {}", id, e);
        }
        return null;
    }

    private MovieResultsPage getMovieResultsPageFromDiscover(Integer page, String language) {
        try {
            Response<MovieResultsPage> movieResultsPageResponse = tmdb.discoverMovie()
                    .language(language)
                    .page(page)
                    .sort_by(DISCOVER_SORT_BY)
                    .build()
                    .execute();

            return movieResultsPageResponse.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MovieResponse> getMovieResponseListFromMovieResultsPage(MovieResultsPage movieResultsPage,
                                                                         String language,
                                                                         Integer limit,
                                                                         Random random) {
        if (movieResultsPage.results == null) {
            throw new MovieNotFoundException(format("Movies not found on discover page %s", movieResultsPage.id));
        }
        List<Integer> movieIdList = movieResultsPage.results.stream()
                .map(baseMovie -> baseMovie.id)
                .toList();

        List<Integer> randomNumbers = getRandomNumbersFromEnumeration(limit, random);

        List<MovieResponse> movieResponseList = new ArrayList<>();
        for (Integer index : randomNumbers) {
            Movie movie = getMovieById(movieIdList.get(index), language);
            if (movie != null) {
                movieResponseList.add(MovieResponse.fromMovie(movie));
            }
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
