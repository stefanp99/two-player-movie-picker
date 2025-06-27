package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class TooManyPlayersException extends RuntimeException {
    public TooManyPlayersException(String seed, String playerSessionId, int numberOfPlayers) {
        super(format("Session with seed %s requested by player session id %s has too many players (%d)", seed, playerSessionId, numberOfPlayers + 1));
    }
}
