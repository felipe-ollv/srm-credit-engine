package com.credit.engine.srm.currency.internal.adapter.out.http;

import com.credit.engine.srm.currency.internal.application.CurrencyProperties;
import com.credit.engine.srm.currency.internal.application.ExchangeRateProvider;
import com.credit.engine.srm.currency.internal.application.ExchangeRateProviderUnavailableException;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class HttpExchangeRateProvider implements ExchangeRateProvider {

    private static final String RATE_PATTERN = "^(?:0|[1-9]\\d{0,8})(?:\\.\\d{1,10})?$";

    private final RestClient restClient;
    private final CurrencyProperties properties;
    private final Clock clock;
    private final RetrySleeper sleeper;

    public HttpExchangeRateProvider(
            RestClient restClient,
            CurrencyProperties properties,
            Clock clock) {
        this(restClient, properties, clock, duration -> Thread.sleep(duration.toMillis()));
    }

    HttpExchangeRateProvider(
            RestClient restClient,
            CurrencyProperties properties,
            Clock clock,
            RetrySleeper sleeper) {
        this.restClient = Objects.requireNonNull(restClient, "restClient is required");
        this.properties = Objects.requireNonNull(properties, "properties is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper is required");
    }

    @Override
    public ExchangeRate fetchUsdToBrl() {
        RestClientException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                ProviderExchangeRateResponse response = restClient.get()
                        .uri("/rates/USD/BRL")
                        .retrieve()
                        .body(ProviderExchangeRateResponse.class);
                return toExchangeRate(response, clock.instant());
            } catch (RestClientException exception) {
                lastFailure = exception;
                if (attempt < properties.maxAttempts()) {
                    pause(properties.retryBackoff());
                }
            } catch (RuntimeException exception) {
                throw new ExchangeRateProviderUnavailableException(
                        "Exchange rate provider returned an invalid response", exception);
            }
        }
        throw new ExchangeRateProviderUnavailableException(
                "Exchange rate provider is unavailable after " + properties.maxAttempts() + " attempts",
                lastFailure);
    }

    private static ExchangeRate toExchangeRate(ProviderExchangeRateResponse response, Instant capturedAt) {
        if (response == null
                || response.baseCurrency() != Currency.USD
                || response.quoteCurrency() != Currency.BRL
                || response.rate() == null
                || !response.rate().matches(RATE_PATTERN)
                || response.effectiveAt() == null) {
            throw new IllegalArgumentException("invalid USD/BRL provider response");
        }
        return new ExchangeRate(
                response.baseCurrency(),
                response.quoteCurrency(),
                new BigDecimal(response.rate()),
                response.effectiveAt(),
                capturedAt);
    }

    private void pause(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateProviderUnavailableException(
                    "Exchange rate refresh was interrupted", exception);
        }
    }
}
