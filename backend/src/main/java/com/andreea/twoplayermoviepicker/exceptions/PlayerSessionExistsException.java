package com.andreea.twoplayermoviepicker.exceptions;

public class PlayerSessionExistsException extends RuntimeException {
    public PlayerSessionExistsException(String playerSessionId) {
        super("Player session id " + playerSessionId + " already exists");
    }
}
