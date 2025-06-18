package com.andreea.twoplayermoviepicker.response_models;

import com.uwetrottmann.tmdb2.entities.Genre;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GenreResponse {
    private Integer id;
    private String name;

    public static GenreResponse fromGenre(Genre genre) {
        return GenreResponse.builder()
                .id(genre.id)
                .name(genre.name)
                .build();
    }
}
