package com.andreea.twoplayermoviepicker.response_models;

import info.movito.themoviedbapi.model.core.Language;
import info.movito.themoviedbapi.model.movies.MovieDb;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.andreea.twoplayermoviepicker.utils.Constants.IMDB_TITLE_BASE_URL;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_IMAGE_BASE_URL;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_MOVIE_PAGE_BASE_URL;

@Builder
@Data
public class MovieResponse {
    private String backDropUrl;
    private List<GenreResponse> genres;
    private Integer id;
    private String imdbUrl;
    private String overview;
    private Double popularity;
    private String posterUrl;
    private String releaseDate;
    private Integer runtime;
    private List<String> spokenLanguages;
    private String status;
    private String tagline;
    private String title;
    private String tmdbUrl;
    private String youtubeTrailer;
    private Double voteAverage;
    private Integer voteCount;

    public static MovieResponse fromMovie(MovieDb movieDb) {
        return MovieResponse.builder()
                .backDropUrl(movieDb.getBackdropPath() != null ? TMDB_IMAGE_BASE_URL.concat(movieDb.getBackdropPath()) : null)
                .genres(movieDb.getGenres() != null ? movieDb.getGenres().stream().map(GenreResponse::fromGenre).toList() : null)
                .id(movieDb.getId())
                .imdbUrl(movieDb.getImdbID() != null ? IMDB_TITLE_BASE_URL.concat(movieDb.getImdbID()) : null)
                .overview(movieDb.getOverview())
                .popularity(movieDb.getPopularity())
                .posterUrl(movieDb.getPosterPath() != null ? TMDB_IMAGE_BASE_URL.concat(movieDb.getPosterPath()) : null)
                .releaseDate(movieDb.getReleaseDate())
                .runtime(movieDb.getRuntime())
                .spokenLanguages(movieDb.getSpokenLanguages() != null ?
                        movieDb.getSpokenLanguages().stream().map(Language::getEnglishName).toList() :
                        null)
                .status(movieDb.getStatus() != null ? movieDb.getStatus() : null)
                .tagline(movieDb.getTagline())
                .title(movieDb.getTitle())
                .tmdbUrl(TMDB_MOVIE_PAGE_BASE_URL.concat(String.valueOf(movieDb.getId())))
                .voteAverage(movieDb.getVoteAverage())
                .voteCount(movieDb.getVoteCount())
                .build();
    }
}
