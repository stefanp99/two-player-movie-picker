package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.services.TmdbService;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor

@CrossOrigin(origins = "${FRONTEND_IP}")
@RestController
@RequestMapping("/api/v1/tmdb")
public class TmdbController {
    private final TmdbService tmdbService;

    /**
     * Retrieves the YouTube trailer for a specific movie in the specified language.
     *
     * @param movieId the unique identifier of the movie for which the trailer is to be fetched
     * @param language the language code in which the trailer is requested
     * @return a ResponseEntity containing the URL of the YouTube trailer as a string,
     *         or an appropriate error response if the trailer cannot be found
     */
    @GetMapping("youtube-trailer/{movieId}/{language}")
    public ResponseEntity<String> getYoutubeTrailer(@PathVariable Integer movieId, @PathVariable String language) throws TmdbException {
        return tmdbService.getYoutubeTrailer(movieId, language);
    }
}
