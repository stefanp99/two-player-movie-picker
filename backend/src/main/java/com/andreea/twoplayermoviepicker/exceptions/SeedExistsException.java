package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class SeedExistsException extends RuntimeException {
    public SeedExistsException(String seed, String playerSessionId) {
        super(format("First seed %s provided by player session id %s already exists", seed, playerSessionId));
    }
}
