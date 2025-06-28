package com.andreea.twoplayermoviepicker.response_models;

import info.movito.themoviedbapi.model.watchproviders.Provider;
import lombok.Builder;

import static com.andreea.twoplayermoviepicker.utils.Constants.TMDB_LOGO_BASE_URL;

@Builder
public record ProviderResponse(String logoUrl,
                               String providerName,
                               Integer providerId) {
    public static ProviderResponse fromProvider(Provider provider) {
        return ProviderResponse.builder()
                .logoUrl(TMDB_LOGO_BASE_URL + provider.getLogoPath())
                .providerName(provider.getProviderName())
                .providerId(provider.getProviderId())
                .build();
    }
}
