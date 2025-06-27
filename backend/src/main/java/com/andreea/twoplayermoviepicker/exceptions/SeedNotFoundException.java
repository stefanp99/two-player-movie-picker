package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class SeedNotFoundException extends RuntimeException {
    public SeedNotFoundException(String seed, String playerSessionId) {
        super(format("First seed %s provided by player session id %s does not exist", seed, playerSessionId));
    }
}
