package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String seed, String playerSessionId) {
        super(format("Session with seed %s requested by player session id %s not found", seed, playerSessionId));
    }
}
