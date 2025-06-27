package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class InvalidSeedException extends RuntimeException {
    public InvalidSeedException(String seed, String playerSessionId) {
        super(format("Invalid seed %s provided by player session id %s", seed, playerSessionId));
    }
}
