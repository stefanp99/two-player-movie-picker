package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.request_models.LikeRequest;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import com.andreea.twoplayermoviepicker.services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    private final SessionService sessionService;

    /**
     * Creates a new room session with the specified parameters provided in the request body.
     *
     * @param request the request containing the initial seed, player session ID, and language preferences
     * @return a ResponseEntity containing a list of MovieResponse objects that are part of the created room session
     */
    @PostMapping("create-room")
    public ResponseEntity<List<MovieResponse>> createRoom(@RequestBody RoomRequest request) {
        return sessionService.createRoom(request);
    }

    /**
     * Allows a player to join an existing room session using the provided request details.
     *
     * @param request the request containing the seed of the room to join, the player session ID,
     *                and the language preferences
     * @return a ResponseEntity containing a list of MovieResponse objects associated with the existing room session,
     *         or an appropriate error response if the room cannot be joined
     */
    @PostMapping("join-room")
    public ResponseEntity<List<MovieResponse>> joinRoom(@RequestBody RoomRequest request) {
        return sessionService.joinRoom(request);
    }

    /**
     * Fetches additional movies for a session based on the provided room details
     * and updates the player's seed index or generates a new seed if necessary.
     *
     * @param request the request containing necessary details about the session including
     *                the seed, player session ID, and language preferences
     * @return a ResponseEntity containing a list of MovieResponse objects representing
     *         the fetched movies or an appropriate error response in case of failure
     */
    @PostMapping("fetch-more")
    public ResponseEntity<List<MovieResponse>> fetchMoreMovies(@RequestBody RoomRequest request) {
        return sessionService.fetchMoreMovies(request);
    }

    /**
     * Adds the specified movie to the player's list of liked movies and determines if the movie
     * is commonly liked by both players in the session.
     *
     * @param request the request containing the player's session ID, the session's seed, and the ID of the movie to be liked
     * @return a ResponseEntity containing a Boolean value, where true indicates that the movie is commonly liked
     *         by both players in the session, and false otherwise
     */
    @PostMapping("add-to-likes")
    public ResponseEntity<Boolean> addToLikesAndReturnIsCommon(@RequestBody LikeRequest request) {
        return sessionService.addToLikesAndReturnIsCommon(request);
    }

    /**
     * Determines whether a player can rejoin a session based on their session ID.
     *
     * @param playerSessionId the unique session identifier of the player attempting to rejoin
     * @return a ResponseEntity containing a Boolean value:
     *         true if the player can rejoin the session, or false otherwise
     */
    @GetMapping("can-player-rejoin")
    public ResponseEntity<Boolean> canPlayerRejoin(@RequestParam String playerSessionId) {
        return sessionService.canPlayerRejoin(playerSessionId);
    }
}
