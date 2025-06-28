package com.andreea.twoplayermoviepicker.request_models;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record RoomRequest(String seed,
                          String playerSessionId,
                          String language,
                          List<Integer> genres,
                          String watchRegion,
                          List<Integer> watchProviders) {
}
