package com.andreea.twoplayermoviepicker.response_models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GenreResponse {
    private Integer id;
    private String name;

    public static GenreResponse fromGenre(info.movito.themoviedbapi.model.core.Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }
}
