package com.andreea.twoplayermoviepicker.services;

import com.andreea.twoplayermoviepicker.exceptions.InvalidSeedException;
import com.andreea.twoplayermoviepicker.exceptions.PlayerNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.PlayerSessionExistsException;
import com.andreea.twoplayermoviepicker.exceptions.SeedExistsException;
import com.andreea.twoplayermoviepicker.exceptions.SeedNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.SessionNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.TooManyPlayersException;
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
import java.util.List;
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
            throw new InvalidSeedException(request.seed(), request.playerSessionId());
        }
        if (playerSessionExists(request.playerSessionId())) {
            throw new PlayerSessionExistsException(request.playerSessionId());
        }
        if (firstSeedExists(request.seed())) {
            throw new SeedExistsException(request.seed(), request.playerSessionId());
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

        return ResponseEntity.ok(tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed()));
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
            throw new InvalidSeedException(request.seed(), request.playerSessionId());
        }
        if (!firstSeedExists(request.seed())) {
            throw new SeedNotFoundException(request.seed(), request.playerSessionId());
        }

        Session session = findSessionBySeed(request.seed());
        if (session == null) {
            throw new SessionNotFoundException(request.seed(), request.playerSessionId());
        }
        if (session.getPlayers().size() >= 2) {//TODO: in the future expand for > 2 players
            throw new TooManyPlayersException(request.seed(), request.playerSessionId(), session.getPlayers().size());
        }

        if (playerSessionExists(request.playerSessionId())) {
            log.info("Player session id {} already exists. Trying to move it to the room", request.playerSessionId());
            Player player = findPlayerBySessionId(request.playerSessionId());
            session.addPlayer(player);
            player.setSession(session);
            player.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Player session id {} moved to the room with ID {}", request.playerSessionId(), session.getId());
            return ResponseEntity.ok(tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed()));
        }

        Player player = Player.builder()
                .playerSessionId(request.playerSessionId())
                .createdAt(LocalDateTime.now())
                .seedIndex(0)
                .build();
        session.addPlayer(player);
        sessionRepository.save(session);

        log.info("Added new player with session id {} to session with id {}", request.playerSessionId(), session.getId());

        return ResponseEntity.ok(tmdbService.getRandomMoviesFromDiscover(request.language(), request.seed()));
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
        Session session = getValidSession(request.seed(), request.playerSessionId());
        Player player = getValidPlayerInSession(request.seed(), request.playerSessionId());

        String lastSeedInSequence = getLastSeedInSequence(session.getId());
        if (lastSeedInSequence == null) {
            throw new RuntimeException("Last seed not found for session " + session.getId());
        }

        player.setSeedIndex(player.getSeedIndex() + 1);
        player.setUpdatedAt(LocalDateTime.now());

        if (player.getSeedIndex() < session.getSeedSequence().size()) {
            String newSeed = session.getSeedSequence().get(player.getSeedIndex());
            sessionRepository.save(session);
            log.info("Player with session ID {} seed index is lower or equal than the number of seeds in the sequence, " +
                    "using seed {} for next request", player.getPlayerSessionId(), newSeed);
            return ResponseEntity.ok(tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed));
        }

        String newSeed = tmdbService.generateSeed(lastSeedInSequence);
        session.addToSeedSequence(newSeed);
        session.setUpdatedAt(LocalDateTime.now());
        log.info("Player with session ID {} seed index is higher than the number of seeds in the sequence, " +
                "adding new seed {} to sequence", player.getPlayerSessionId(), newSeed);
        sessionRepository.save(session);

        return ResponseEntity.ok(tmdbService.getRandomMoviesFromDiscover(request.language(), newSeed));
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
        Session session = getValidSession(request.seed(), request.playerSessionId());
        Player player = getValidPlayerInSession(request.seed(), request.playerSessionId());
        String movieId = String.valueOf(request.movieId());

        if (player.getLikes().contains(movieId)) {
            throw new IllegalArgumentException("Movie already liked");
        }

        player.addToLikes(movieId);
        player.setUpdatedAt(LocalDateTime.now());
        log.info("Added movie {} to likes for player {}", movieId, player.getPlayerSessionId());

        Player otherPlayer = findOtherPlayerInSession(player, session);
        if (otherPlayer == null) {
            log.info("No other player found in session {}. Current player session ID {}",
                    session.getId(), player.getPlayerSessionId());
            return ResponseEntity.ok(false);
        }

        if (otherPlayer.getLikes().contains(movieId)) {
            log.info("Common like found for movie {} by player session id {}", movieId, player.getPlayerSessionId());
            session.addToCommonLikes(movieId);
            sessionRepository.save(session);
            return ResponseEntity.ok(true);
        }
        // No common likes found between player and otherPlayer
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
     * if the session is valid, or a 404 NOT FOUND response if the session is invalid or not found.
     */
    public ResponseEntity<List<Integer>> getCommonLikes(String playerSessionId) {
        Session session = getValidSessionByPlayerSessionId(playerSessionId);
        return ResponseEntity.ok(session.getCommonLikes().stream()
                .map(Integer::parseInt)
                .toList());
    }

    /**
     * Checks if a room exists based on the provided seed.
     *
     * @param seed the unique identifier used to determine if a room exists
     * @return a ResponseEntity containing a Boolean value; true if the room exists, false otherwise
     */
    public ResponseEntity<Boolean> doesRoomExist(String seed) {
        if (firstSeedExists(seed)) {
            return ResponseEntity.ok(true);
        }
        return ResponseEntity.ok(false);
    }

    private Session getValidSession(String seed, String playerSessionId) {
        if (!isSeedValid(seed)) {
            throw new InvalidSeedException(seed, playerSessionId);
        }
        return sessionRepository.findSessionIdByFirstSeed(seed)
                .flatMap(sessionRepository::findById)
                .orElseThrow(() -> new SessionNotFoundException(seed, playerSessionId));
    }

    private Session getValidSessionByPlayerSessionId(String playerSessionId) {
        Player player = getValidPlayer(playerSessionId);
        return player.getSession();
    }

    private Player getValidPlayer(String playerSessionId) {
        return playerRepository.findByPlayerSessionId(playerSessionId)
                .orElseThrow(() -> new PlayerNotFoundException(playerSessionId));
    }

    private Player getValidPlayerInSession(String seed, String playerSessionId) {
        Player player = getValidPlayer(playerSessionId);
        Session session = player.getSession();
        if (session == null || session.getSeedSequence().isEmpty()
                || !seed.equals(session.getSeedSequence().getFirst())) {
            throw new SessionNotFoundException(seed, playerSessionId);
        }
        return player;
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
