package com.credit.engine.srm.currency.internal.application;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class CurrencyServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private static final Duration MAX_AGE = Duration.ofHours(24);

    @Test
    void shouldFindCurrentRateUsingConfiguredMaximumAge() {
        ExchangeRate rate = rate("5.4321", NOW.minusSeconds(60), NOW);
        TrackingRepository repository = new TrackingRepository();
        repository.current = Optional.of(rate);
        CurrencyService service = new CurrencyService(repository, () -> rate, properties());

        assertThat(service.find(Currency.USD, Currency.BRL, NOW)).contains(rate);
        assertThat(repository.requestedBase).isEqualTo(Currency.USD);
        assertThat(repository.requestedQuote).isEqualTo(Currency.BRL);
        assertThat(repository.requestedAt).isEqualTo(NOW);
        assertThat(repository.requestedMaxAge).isEqualTo(MAX_AGE);
    }

    @Test
    void shouldRefreshAndPersistImmutableProviderSnapshot() {
        ExchangeRate rate = rate("5.5000", NOW.minusSeconds(1), NOW);
        TrackingRepository repository = new TrackingRepository();
        CurrencyService service = new CurrencyService(repository, () -> rate, properties());

        assertThat(service.refreshUsdToBrl()).isEqualTo(rate);
        assertThat(repository.saved).isSameAs(rate);
    }

    @Test
    void shouldRejectPairWithEqualCurrencies() {
        TrackingRepository repository = new TrackingRepository();
        CurrencyService service = new CurrencyService(repository, () -> null, properties());
        assertThatThrownBy(() -> service.find(Currency.BRL, Currency.BRL, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exchange rate currencies must be different");
    }

    @Test
    void shouldPreserveRepositoryWhenProviderRefreshFails() {
        TrackingRepository repository = new TrackingRepository();
        CurrencyService service = new CurrencyService(
                repository,
                () -> { throw new ExchangeRateProviderUnavailableException("provider unavailable"); },
                properties());

        assertThatThrownBy(service::refreshUsdToBrl)
                .isInstanceOf(ExchangeRateProviderUnavailableException.class)
                .hasMessage("provider unavailable");
        assertThat(repository.saved).isNull();
    }

    private static CurrencyProperties properties() {
        return new CurrencyProperties(
                "http://localhost:8082",
                Duration.ofMillis(500),
                Duration.ofMillis(500),
                3,
                Duration.ofMillis(250),
                MAX_AGE,
                false);
    }

    private static ExchangeRate rate(String value, Instant effectiveAt, Instant capturedAt) {
        return new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal(value),
                effectiveAt,
                capturedAt);
    }

    private static final class TrackingRepository implements ExchangeRateRepository {

        private Optional<ExchangeRate> current = Optional.empty();
        private ExchangeRate saved;
        private Currency requestedBase;
        private Currency requestedQuote;
        private Instant requestedAt;
        private Duration requestedMaxAge;

        @Override
        public ExchangeRate save(ExchangeRate exchangeRate) {
            saved = exchangeRate;
            return exchangeRate;
        }

        @Override
        public Optional<ExchangeRate> findCurrent(
                Currency baseCurrency,
                Currency quoteCurrency,
                Instant at,
                Duration maxAge) {
            requestedBase = baseCurrency;
            requestedQuote = quoteCurrency;
            requestedAt = at;
            requestedMaxAge = maxAge;
            return current;
        }
    }
}
