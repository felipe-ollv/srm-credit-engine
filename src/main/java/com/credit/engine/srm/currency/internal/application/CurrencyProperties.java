package com.credit.engine.srm.currency.internal.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.currency")
public record CurrencyProperties(
        String providerBaseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryBackoff,
        Duration maxAge,
        boolean bootstrapEnabled) {

    public CurrencyProperties {
        if (providerBaseUrl == null || providerBaseUrl.isBlank()) {
            throw new IllegalArgumentException("providerBaseUrl is required");
        }
        requirePositive(connectTimeout, "connectTimeout");
        requirePositive(readTimeout, "readTimeout");
        requirePositive(retryBackoff, "retryBackoff");
        requirePositive(maxAge, "maxAge");
        if (maxAttempts < 1 || maxAttempts > 5) {
            throw new IllegalArgumentException("maxAttempts must be between 1 and 5");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
