package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.models.Player;
import com.andreea.twoplayermoviepicker.models.Session;
import com.andreea.twoplayermoviepicker.repositories.PlayerRepository;
import com.andreea.twoplayermoviepicker.repositories.SessionRepository;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.andreea.twoplayermoviepicker.utils.UtilityMethods.isSeedValid;
import static org.springframework.http.HttpStatus.CREATED;

@RequiredArgsConstructor

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final PlayerRepository playerRepository;

    public ResponseEntity<String> createRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            return ResponseEntity.badRequest().body("Seed must be a 4-digit alphanumeric string.");
        }
        if (playerSessionExists(request.playerSessionId())) {
            return ResponseEntity.badRequest().body("Player session id already exists.");
        }

        Player player = Player.builder()
                .playerSessionId(request.playerSessionId())
                .createdAt(LocalDateTime.now())
                .build();

        Session session = Session.builder()
                .createdAt(LocalDateTime.now())
                .build();
        session.addPlayer(player);
        session.addToSeedSequence(request.seed());

        sessionRepository.save(session);

        return ResponseEntity.status(CREATED).body("Session created with seed " + request.seed());
    }

    public ResponseEntity<String> joinRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            return ResponseEntity.badRequest().body("Seed must be a 4-digit alphanumeric string.");
        }
        if (playerSessionExists(request.playerSessionId())) {
            return ResponseEntity.badRequest().body("Player session id already exists.");
        }

        Optional<Session> optionalSession = sessionRepository.findByFirstSeed(request.seed());
        if (optionalSession.isPresent()) {
            Session session = optionalSession.get();
            Player player = Player.builder()
                    .playerSessionId(request.playerSessionId())
                    .createdAt(LocalDateTime.now())
                    .build();
            session.addPlayer(player);
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);

            return ResponseEntity.ok("Session joined with seed " + request.seed());
        }

        return ResponseEntity.notFound().build();
    }

    private Boolean playerSessionExists(String playerSessionId) {
        return playerRepository.findByPlayerSessionId(playerSessionId).isPresent();
    }
}
