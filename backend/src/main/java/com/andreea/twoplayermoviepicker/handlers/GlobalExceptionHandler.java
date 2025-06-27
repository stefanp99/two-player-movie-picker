package com.andreea.twoplayermoviepicker.handlers;

import com.andreea.twoplayermoviepicker.exceptions.InvalidSeedException;
import com.andreea.twoplayermoviepicker.exceptions.MovieNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.PlayerNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.PlayerSessionExistsException;
import com.andreea.twoplayermoviepicker.exceptions.SeedExistsException;
import com.andreea.twoplayermoviepicker.exceptions.SeedNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.SessionNotFoundException;
import com.andreea.twoplayermoviepicker.exceptions.TooManyPlayersException;
import info.movito.themoviedbapi.tools.TmdbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidSeedException.class)
    public ResponseEntity<String> handleInvalidSeedException(InvalidSeedException ex) {
        log.warn("Invalid seed encountered: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<String> handleMovieNotFoundException(MovieNotFoundException ex) {
        log.warn("Movie not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(PlayerSessionExistsException.class)
    public ResponseEntity<String> handlePlayerSessionExistsException(PlayerSessionExistsException ex) {
        log.warn("Player session ID already exists: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(SeedExistsException.class)
    public ResponseEntity<String> handleSeedExistsException(SeedExistsException ex) {
        log.warn("Seed already exists: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(TmdbException.class)
    public ResponseEntity<String> handleTmdbException(TmdbException ex) {
        log.error("TMDB error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(SeedNotFoundException.class)
    public ResponseEntity<String> handleSeedDoesNotExistException(SeedNotFoundException ex) {
        log.warn("Seed does not exist: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<String> handleSessionNotFoundException(SessionNotFoundException ex) {
        log.warn("Session not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(TooManyPlayersException.class)
    public ResponseEntity<String> handleTooManyPlayersException(TooManyPlayersException ex) {
        log.warn("Too many players in session: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<String> handlePlayerNotFoundException(PlayerNotFoundException ex) {
        log.warn("Player not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
