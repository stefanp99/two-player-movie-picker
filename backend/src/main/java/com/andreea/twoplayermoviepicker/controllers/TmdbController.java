package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.services.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/tmdb")
public class TmdbController {
    private final TmdbService tmdbService;

    @GetMapping("youtube-trailer/{movieId}/{language}")
    public ResponseEntity<String> getYoutubeTrailer(@PathVariable Integer movieId, @PathVariable String language) {
        return tmdbService.getYoutubeTrailer(movieId, language);
    }
}
