package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.response_models.GenreResponse;
import com.andreea.twoplayermoviepicker.response_models.ProviderResponse;
import com.andreea.twoplayermoviepicker.services.TmdbService;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor

@CrossOrigin(origins = "${FRONTEND_IP}")
@RestController
@RequestMapping("/api/v1/tmdb")
public class TmdbController {
    private final TmdbService tmdbService;

    /**
     * Retrieves the YouTube trailer for a specific movie in the specified language.
     *
     * @param movieId  the unique identifier of the movie for which the trailer is to be fetched
     * @param language the language code in which the trailer is requested
     * @return a ResponseEntity containing the URL of the YouTube trailer as a string,
     * or an appropriate error response if the trailer cannot be found
     */
    @GetMapping("youtube-trailer/{movieId}/{language}")
    public ResponseEntity<String> getYoutubeTrailer(@PathVariable Integer movieId, @PathVariable String language) throws TmdbException {
        return tmdbService.getYoutubeTrailer(movieId, language);
    }

    /**
     * Retrieves a list of watch providers available for movies based on the specified region and language.
     *
     * @param watchRegion the region code (e.g., country ISO code) for which the watch provider data is requested
     * @param language the language code in which the data is requested
     * @return a ResponseEntity containing a list of ProviderResponse objects,
     *         which include details such as the provider's name, logo path, and ID
     * @throws TmdbException if an error occurs while fetching the watch provider data
     */
    @GetMapping("watch-providers/{watchRegion}/{language}")
    public ResponseEntity<List<ProviderResponse>> getWatchProviders(@PathVariable String watchRegion, @PathVariable String language) throws TmdbException {
        return tmdbService.getWatchProviders(watchRegion, language);
    }

    /**
     * Retrieves a list of movie genres.
     *
     * @return a ResponseEntity containing a list of GenreResponse objects,
     *         each representing a movie genre with its corresponding ID and name
     * @throws TmdbException if an error occurs while fetching the genres
     */
    @GetMapping("genres")
    public ResponseEntity<List<GenreResponse>> getGenresResponse() throws TmdbException {
        return tmdbService.getGenresResponse();
    }
}
