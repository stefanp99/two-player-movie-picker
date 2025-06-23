package com.andreea.twoplayermoviepicker.response_models;

import info.movito.themoviedbapi.model.core.Genre;
import lombok.Builder;

@Builder
public record GenreResponse(Integer id, String name) {
    public static GenreResponse fromGenre(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }
}
