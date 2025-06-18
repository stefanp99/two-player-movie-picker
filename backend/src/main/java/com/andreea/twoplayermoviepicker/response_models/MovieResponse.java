package com.andreea.twoplayermoviepicker.response_models;

import com.uwetrottmann.tmdb2.entities.Movie;
import lombok.Builder;
import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.List;

import static com.andreea.twoplayermoviepicker.utils.Constants.IMDB_TITLE_BASE_URL;
import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_IMAGE_BASE_URL;

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
    private Double voteAverage;
    private Integer voteCount;

    public static MovieResponse fromMovie(Movie movie) {
        return MovieResponse.builder()
                .backDropUrl(movie.backdrop_path != null ? TMDB_IMAGE_BASE_URL.concat(movie.backdrop_path) : null)
                .genres(movie.genres != null ? movie.genres.stream().map(GenreResponse::fromGenre).toList() : null)
                .id(movie.id)
                .imdbUrl(movie.imdb_id != null ? IMDB_TITLE_BASE_URL.concat(movie.imdb_id) : null)
                .overview(movie.overview)
                .popularity(movie.popularity)
                .posterUrl(movie.poster_path != null ? TMDB_IMAGE_BASE_URL.concat(movie.poster_path) : null)
                .releaseDate(movie.release_date != null ?
                        new SimpleDateFormat("yyyy-MM-dd").format(movie.release_date) : null)
                .runtime(movie.runtime)
                .spokenLanguages(movie.spoken_languages != null ?
                        movie.spoken_languages.stream().map(spokenLanguage -> spokenLanguage.name).toList() :
                        null)
                .status(movie.status != null ? movie.status.value : null)
                .tagline(movie.tagline)
                .title(movie.title)
                .voteAverage(movie.vote_average)
                .voteCount(movie.vote_count)
                .build();
    }
}
