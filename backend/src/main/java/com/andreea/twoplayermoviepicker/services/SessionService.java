package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.models.Player;
import com.andreea.twoplayermoviepicker.models.Session;
import com.andreea.twoplayermoviepicker.repositories.PlayerRepository;
import com.andreea.twoplayermoviepicker.repositories.SessionRepository;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.andreea.twoplayermoviepicker.utils.UtilityMethods.isSeedValid;

@RequiredArgsConstructor

@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final TmdbService tmdbService;

    public ResponseEntity<List<MovieResponse>> createRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            return ResponseEntity.badRequest().build();
        }
        if (playerSessionExists(request.playerSessionId())) {
            return ResponseEntity.badRequest().build();
        }
        if (firstSeedExists(request.seed())) {
            return ResponseEntity.badRequest().build();
        }

        Player player = Player.builder()
                .playerSessionId(request.playerSessionId())
                .createdAt(LocalDateTime.now())
                .seedIndex(0)
                .build();

        Session session = Session.builder()
                .createdAt(LocalDateTime.now())
                .build();
        session.addPlayer(player);
        session.addToSeedSequence(request.seed());

        sessionRepository.save(session);

        return tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed());
    }

    public ResponseEntity<List<MovieResponse>> joinRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            return ResponseEntity.badRequest().build();
        }
        if (playerSessionExists(request.playerSessionId())) {
            return ResponseEntity.badRequest().build();
        }
        if (!firstSeedExists(request.seed())) {
            return ResponseEntity.notFound().build();
        }

        if (firstSeedExists(request.seed())) {
            Optional<Integer> optionalId = sessionRepository.findSessionIdByFirstSeed(request.seed());
            if (optionalId.isPresent()) {
                Session session = sessionRepository.findById(optionalId.get()).orElseThrow();
                Player player = Player.builder()
                        .playerSessionId(request.playerSessionId())
                        .createdAt(LocalDateTime.now())
                        .seedIndex(0)
                        .build();
                session.addPlayer(player);
                sessionRepository.save(session);

                return tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed());
            }
        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<MovieResponse>> fetchMoreMovies(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            return ResponseEntity.badRequest().build();
        }
        if (!playerSessionExists(request.playerSessionId())) {
            return ResponseEntity.notFound().build();
        }
        if (!firstSeedExists(request.seed())) {
            return ResponseEntity.notFound().build();
        }

        Optional<Integer> optionalId = sessionRepository.findSessionIdByFirstSeed(request.seed());
        if (optionalId.isPresent()) {
            Session session = sessionRepository.findById(optionalId.get()).orElseThrow();
            Optional<String> optionalLastSeedInSequence = sessionRepository.findLastSeedInSequenceById(session.getId());
            if (optionalLastSeedInSequence.isPresent()) {
                Optional<Player> optionalPlayer = playerRepository.findByPlayerSessionId(request.playerSessionId());
                if (optionalPlayer.isPresent()) {
                    Player player = optionalPlayer.get();
                    player.setSeedIndex(player.getSeedIndex() + 1);
                    player.setUpdatedAt(LocalDateTime.now());

                    if (player.getSeedIndex() + 1 <= session.getSeedSequence().size()) {
                        String newSeed = session.getSeedSequence().get(player.getSeedIndex());
                        sessionRepository.save(session);
                        return tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed);
                    }

                    String newSeed = tmdbService.generateSeed(optionalLastSeedInSequence.get());

                    session.addToSeedSequence(newSeed);
                    session.setUpdatedAt(LocalDateTime.now());
                    sessionRepository.save(session);

                    return tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed);
                }
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.notFound().build();
    }

    private Boolean playerSessionExists(String playerSessionId) {
        return playerRepository.findByPlayerSessionId(playerSessionId).isPresent();
    }

    private Boolean firstSeedExists(String seed) {
        return sessionRepository.findSessionIdByFirstSeed(seed).isPresent();
    }
}
