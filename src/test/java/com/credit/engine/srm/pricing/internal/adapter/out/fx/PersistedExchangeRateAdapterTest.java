package com.credit.engine.srm.pricing.internal.adapter.out.fx;

import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
class PersistedExchangeRateAdapterTest {

    @Test
    void shouldDelegateCurrentUsdToBrlLookupToCurrencyModule() {
        Instant at = Instant.parse("2026-08-27T12:00:00Z");
        ExchangeRate rate = new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal("5.4321"),
                at.minusSeconds(60),
                at);
        Currency[] requestedCurrencies = new Currency[2];
        Instant[] requestedAt = new Instant[1];
        FindCurrentExchangeRateUseCase useCase = (baseCurrency, quoteCurrency, lookupAt) -> {
            requestedCurrencies[0] = baseCurrency;
            requestedCurrencies[1] = quoteCurrency;
            requestedAt[0] = lookupAt;
            return Optional.of(rate);
        };

        assertThat(new PersistedExchangeRateAdapter(useCase).findValidUsdToBrl(at))
                .contains(rate);
        assertThat(requestedCurrencies).containsExactly(Currency.USD, Currency.BRL);
        assertThat(requestedAt[0]).isEqualTo(at);
    }
}
