package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.request_models.LikeRequest;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import com.andreea.twoplayermoviepicker.services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    private final SessionService sessionService;

    @PostMapping("create-room")
    public ResponseEntity<List<MovieResponse>> createRoom(@RequestBody RoomRequest request) {
        return sessionService.createRoom(request);
    }

    @PostMapping("join-room")
    public ResponseEntity<List<MovieResponse>> joinRoom(@RequestBody RoomRequest request) {
        return sessionService.joinRoom(request);
    }

    @PostMapping("fetch-more")
    public ResponseEntity<List<MovieResponse>> fetchMoreMovies(@RequestBody RoomRequest request) {
        return sessionService.fetchMoreMovies(request);
    }

    @PostMapping("add-to-likes")
    public ResponseEntity<Boolean> addToLikesAndReturnIsCommon(@RequestBody LikeRequest request) {
        return sessionService.addToLikesAndReturnIsCommon(request);
    }
}
