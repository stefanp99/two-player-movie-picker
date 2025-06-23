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

    /**
     * Creates a new room based on the provided request. The method validates the provided seed,
     * ensures the player session ID and seed do not already exist, and creates a new session
     * with the specified seed and player information. If the seed or player session ID is invalid
     * or already exists, a bad request response is returned. Otherwise, the created session is persisted
     * and a list of movies retrieved from the TMDb service is returned.
     *
     * @param request the request containing details such as seed, language, and player session ID
     *                required for creating the room
     * @return a {@link ResponseEntity} containing the list of {@link MovieResponse} if the room
     * is successfully created, or a bad request response if validation fails
     */
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

    /**
     * Allows a player to join an existing room based on the provided room request details.
     * Validates the request data, checks the room's availability, and adds the player to the room if all conditions are met.
     *
     * @param request The request object containing details about the room to join, including the seed, player session ID,
     *                and language preferences.
     * @return A ResponseEntity containing a list of MovieResponse objects that are randomly retrieved
     * based on the room's seed and language preferences. Returns a bad request or not found response
     * based on validation or room availability.
     */
    public ResponseEntity<List<MovieResponse>> joinRoom(RoomRequest request) {
        if (!isSeedValid(request.seed())) {
            log.warn("Invalid seed provided: {}", request.seed());
            return ResponseEntity.badRequest().build();
        }
        if (!firstSeedExists(request.seed())) {
            log.warn("Seed {} does not exist", request.seed());
            return ResponseEntity.notFound().build();
        }

        Session session = findSessionBySeed(request.seed());
        if (session == null) {
            log.warn("Session with seed {} not found", request.seed());
            return ResponseEntity.notFound().build();
        }
        if (session.getPlayers().size() >= 2) {//TODO: in the future expand for > 2 players
            log.warn("Session with seed {} already has 2 players", request.seed());
            return ResponseEntity.badRequest().build();
        }

        if (playerSessionExists(request.playerSessionId())) {
            log.info("Player session id {} already exists. Trying to move it to the room", request.playerSessionId());
            Player player = findPlayerBySessionId(request.playerSessionId());
            session.addPlayer(player);
            player.setSession(session);
            player.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Player session id {} moved to the room with ID {}", request.playerSessionId(), session.getId());
            return tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed());
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

    /**
     * Fetches more movies based on the provided room request by validating the session
     * and player information, determining the next seed, and invoking the appropriate
     * third-party service to retrieve random movie recommendations.
     *
     * @param request the room request containing necessary information, such as session and
     *                language details, to retrieve more movies
     * @return a ResponseEntity containing a list of movie responses if successful, or an
     * appropriate error response if validation fails or an error occurs
     */
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

    /**
     * Adds the specified movie to the "likes" of the player issuing the request and checks
     * whether it is a common like between the player and another player in the same session.
     * Updates the session and player data accordingly.
     *
     * @param request the request containing the session ID, player ID, and movie ID to be added
     *                to the player's likes
     * @return a ResponseEntity containing a Boolean:
     * - true if the movie is a common like between the two players in the session,
     * meaning the other player has already liked the same movie,
     * - false otherwise,
     * - or a response with appropriate HTTP status codes if validation or processing fails
     */
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

        Player otherPlayer = findOtherPlayerInSession(player, session);
        if (otherPlayer == null) {
            log.warn("No other player found in session {}. Current player session ID {}",
                    session.getId(), player.getPlayerSessionId());
            return ResponseEntity.ok(false);
        }

        if (otherPlayer.getLikes().contains(movieId)) {
            log.info("Common like found for movie {} by player session id {}", movieId, player.getPlayerSessionId());
            session.addToCommonLikes(movieId);
            sessionRepository.save(session);
            return ResponseEntity.ok(true);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return ResponseEntity.ok(false);
    }

    /**
     * Determines whether a player can rejoin a session based on their session ID.
     *
     * @param playerSessionId the unique identifier associated with the player's session
     * @return a ResponseEntity containing a Boolean value:
     * true if the player can rejoin, false otherwise
     */
    public ResponseEntity<Boolean> canPlayerRejoin(String playerSessionId) {
        Optional<Player> optionalPlayer = playerRepository.findByPlayerSessionId(playerSessionId);
        if (optionalPlayer.isEmpty()) {
            log.info("Player with session id {} not found, therefore player can NOT rejoin", playerSessionId);
            return ResponseEntity.ok(false);
        }
        Player player = optionalPlayer.get();
        if (player.getSession() == null) {
            log.info("Player with session id {} has no session, therefore player can NOT rejoin", playerSessionId);
            return ResponseEntity.ok(false);
        }

        return ResponseEntity.ok(true);
    }

    /**
     * Retrieves a list of common likes for a given player's session.
     *
     * @param playerSessionId The unique identifier of the player's session.
     * @return A ResponseEntity containing a list of integers representing common likes
     *         if the session is valid, or a 404 NOT FOUND response if the session is invalid or not found.
     */
    public ResponseEntity<List<Integer>> getCommonLikes(String playerSessionId) {
        Optional<Map.Entry<Session, Player>> result = validateAndFetch(playerSessionId);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Session session = result.get().getKey();
        return ResponseEntity.ok(session.getCommonLikes().stream()
                .map(Integer::parseInt)
                .toList());
    }

    private Optional<Map.Entry<Session, Player>> validateAndFetch(String playerSessionId) {
        Optional<Player> optionalPlayer = playerRepository.findByPlayerSessionId(playerSessionId);
        if (optionalPlayer.isEmpty()) {
            log.warn("Player with session id {} not found", playerSessionId);
            return Optional.empty();
        }
        Player player = optionalPlayer.get();
        if (player.getSession() == null) {
            log.warn("Player with session id {} has no session", playerSessionId);
            return Optional.empty();
        }
        Session session = player.getSession();
        return validateCommon(session.getSeedSequence().getFirst(), playerSessionId);
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

    private Player findOtherPlayerInSession(Player currentPlayer, Session session) {
        for (Player player : session.getPlayers()) {
            if (!player.getPlayerSessionId().equals(currentPlayer.getPlayerSessionId())) {
                return player;
            }
        }
        return null;
    }
}
