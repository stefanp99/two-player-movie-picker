package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import com.andreea.twoplayermoviepicker.services.TmdbService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/tmdb")
public class TmdbController {
    private final TmdbService tmdbService;

    @GetMapping("fetch")
    public ResponseEntity<List<MovieResponse>> getInitialMovies(@RequestParam String language,
                                                                @RequestParam Integer limit,
                                                                @RequestParam String seed) {
        return tmdbService.getRandomMoviesFromDiscover(language, limit, seed);
    }

    @GetMapping("generate-seed/{oldSeed}")
    public ResponseEntity<String> generateNewSeed(@PathVariable String oldSeed) {
        return tmdbService.generateSeed(oldSeed);
    }
}
