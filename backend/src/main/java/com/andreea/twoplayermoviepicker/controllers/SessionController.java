package com.andreea.twoplayermoviepicker.controllers;

import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.services.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    private final SessionService sessionService;

    @PostMapping("create-room")
    public ResponseEntity<String> createRoom(@RequestBody RoomRequest request) {
        return sessionService.createRoom(request);
    }

    @PostMapping("join-room")
    public ResponseEntity<String> joinRoom(@RequestBody RoomRequest request) {
        return sessionService.joinRoom(request);
    }
}
