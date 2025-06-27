package com.andreea.twoplayermoviepicker.exceptions;

import static java.lang.String.format;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String playerSessionId) {
        super(format("Player with  session id %s not found", playerSessionId));
    }
}
