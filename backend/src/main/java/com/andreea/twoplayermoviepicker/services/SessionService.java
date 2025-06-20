package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.models.Player;
import com.andreea.twoplayermoviepicker.models.Session;
import com.andreea.twoplayermoviepicker.repositories.PlayerRepository;
import com.andreea.twoplayermoviepicker.repositories.SessionRepository;
import com.andreea.twoplayermoviepicker.request_models.LikeRequest;
import com.andreea.twoplayermoviepicker.request_models.RoomRequest;
import com.andreea.twoplayermoviepicker.response_models.MovieResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.andreea.twoplayermoviepicker.utils.UtilityMethods.isSeedValid;

@RequiredArgsConstructor

@Slf4j
@Service
public class SessionService {
    private final SessionRepository sessionRepository;
    private final PlayerRepository playerRepository;
    private final TmdbService tmdbService;

    public ResponseEntity<List<MovieResponse>> createRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            log.warn("Invalid seed provided: {}", request.seed());
            return ResponseEntity.badRequest().build();
        }
        if (playerSessionExists(request.playerSessionId())) {
            log.warn("Player session id {} already exists", request.playerSessionId());
            return ResponseEntity.badRequest().build();
        }
        if (firstSeedExists(request.seed())) {
            log.warn("Seed {} already exists", request.seed());
            return ResponseEntity.badRequest().build();
        }

        Player player = Player.builder()
                .playerSessionId(request.playerSessionId())
                .createdAt(LocalDateTime.now())
                .seedIndex(0)
                .build();

        log.info("Created new player with session id {}", request.playerSessionId());

        Session session = Session.builder()
                .createdAt(LocalDateTime.now())
                .build();
        session.addPlayer(player);
        session.addToSeedSequence(request.seed());

        log.info("Created new session with seed {}", request.seed());

        sessionRepository.save(session);

        return tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed());
    }

    public ResponseEntity<List<MovieResponse>> joinRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            log.warn("Invalid seed provided: {}", request.seed());
            return ResponseEntity.badRequest().build();
        }
        if (playerSessionExists(request.playerSessionId())) {
            log.warn("Player session id {} already exists", request.playerSessionId());
            return ResponseEntity.badRequest().build();
        }
        if (!firstSeedExists(request.seed())) {
            return ResponseEntity.notFound().build();
        }

        if (firstSeedExists(request.seed())) {
            Session session = findSessionBySeed(request.seed());
            if (session == null) {
                return ResponseEntity.notFound().build();
            }
            Player player = Player.builder()
                    .playerSessionId(request.playerSessionId())
                    .createdAt(LocalDateTime.now())
                    .seedIndex(0)
                    .build();
            session.addPlayer(player);
            sessionRepository.save(session);

            log.info("Added new player with session id {} to session with id {}", request.playerSessionId(), session.getId());

            return tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed());

        }

        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<List<MovieResponse>> fetchMoreMovies(RoomRequest request) {
        Optional<Map.Entry<Session, Player>> result = validateAndFetch(request);
        if (result.isEmpty()) {
            log.warn("Session or player not found for room request {}", request);
            return ResponseEntity.notFound().build();
        }

        Session session = result.get().getKey();
        Player player = result.get().getValue();

        String lastSeedInSequence = getLastSeedInSequence(session.getId());
        if (lastSeedInSequence == null) {
            log.error("Last seed in sequence is null for session id {}", session.getId());
            return ResponseEntity.internalServerError().build();
        }

        player.setSeedIndex(player.getSeedIndex() + 1);
        player.setUpdatedAt(LocalDateTime.now());

        if (player.getSeedIndex() + 1 <= session.getSeedSequence().size()) {
            String newSeed = session.getSeedSequence().get(player.getSeedIndex());
            sessionRepository.save(session);
            log.info("Player seed index is lower or equal than the number of seeds in the sequence, " +
                    "using seed {} for next request", newSeed);
            return tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed);
        }

        String newSeed = tmdbService.generateSeed(lastSeedInSequence);
        session.addToSeedSequence(newSeed);
        session.setUpdatedAt(LocalDateTime.now());
        log.info("Player seed index is higher than the number of seeds in the sequence, " +
                "adding new seed {} to sequence", newSeed);
        sessionRepository.save(session);

        return tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed);
    }


    public ResponseEntity<Boolean> addToLikesAndReturnIsCommon(LikeRequest request) {
        Optional<Map.Entry<Session, Player>> result = validateAndFetch(request);
        if (result.isEmpty()) {
            log.warn("Session or player not found for like request {}", request);
            return ResponseEntity.notFound().build();
        }

        Session session = result.get().getKey();
        Player player = result.get().getValue();

        String movieId = String.valueOf(request.movieId());

        if (player.getLikes().contains(movieId)) {
            log.warn("Movie {} already liked by player {}", movieId, player.getPlayerSessionId());
            return ResponseEntity.badRequest().build();
        }

        player.addToLikes(movieId);
        player.setUpdatedAt(LocalDateTime.now());
        log.info("Added movie {} to likes for player {}", movieId, player.getPlayerSessionId());

        if (session.getAllLikes().contains(movieId)) {
            log.info("Common like found for movie {} by player session id {}", movieId, player.getPlayerSessionId());
            sessionRepository.save(session);
            return ResponseEntity.ok(true);
        }
        session.addToAllLikes(movieId);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Added movie {} to all likes for player session id {}", movieId, player.getPlayerSessionId());
        return ResponseEntity.ok(false);
    }

    private Optional<Map.Entry<Session, Player>> validateAndFetch(RoomRequest request) {
        return validateCommon(request.seed(), request.playerSessionId());
    }

    private Optional<Map.Entry<Session, Player>> validateAndFetch(LikeRequest request) {
        return validateCommon(request.seed(), request.playerSessionId());
    }

    private Optional<Map.Entry<Session, Player>> validateCommon(String seed, String playerSessionId) {
        if (!isSeedValid(seed) || !playerSessionExists(playerSessionId) || !firstSeedExists(seed)) {
            return Optional.empty();
        }

        Session session = findSessionBySeed(seed);
        Player player = findPlayerBySessionId(playerSessionId);
        if (session == null || player == null) {
            return Optional.empty();
        }

        return Optional.of(new AbstractMap.SimpleEntry<>(session, player));
    }

    private Boolean playerSessionExists(String playerSessionId) {
        return playerRepository.findByPlayerSessionId(playerSessionId).isPresent();
    }

    private Boolean firstSeedExists(String seed) {
        return sessionRepository.findSessionIdByFirstSeed(seed).isPresent();
    }

    private Session findSessionBySeed(String seed) {
        Optional<Integer> optionalId = sessionRepository.findSessionIdByFirstSeed(seed);
        return optionalId
                .flatMap(sessionRepository::findById)
                .orElse(null);
    }

    private String getLastSeedInSequence(Integer sessionId) {
        return sessionRepository.findLastSeedInSequenceById(sessionId).orElse(null);
    }

    private Player findPlayerBySessionId(String playerSessionId) {
        return playerRepository.findByPlayerSessionId(playerSessionId).orElse(null);
    }
}
