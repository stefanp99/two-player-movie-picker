package com.andreea.twoplayermoviepicker.request_models;

public record LikeRequest(String seed,
                          String playerSessionId,
                          Integer movieId) {
}
