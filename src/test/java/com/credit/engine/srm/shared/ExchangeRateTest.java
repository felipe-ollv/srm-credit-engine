package com.credit.engine.srm.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExchangeRateTest {

    private static final Instant EFFECTIVE_AT = Instant.parse("2026-08-26T12:00:00Z");
    private static final Instant CAPTURED_AT = Instant.parse("2026-08-26T12:01:00Z");

    @Test
    void shouldConvertInDirectAndInverseDirections() {
        ExchangeRate rate = usdToBrl("5.4321");

        assertThat(rate.convert(Money.of("10.00", Currency.USD), Currency.BRL))
                .isEqualTo(Money.of("54.32", Currency.BRL));
        assertThat(rate.convert(Money.of("92859.94", Currency.BRL), Currency.USD))
                .isEqualTo(Money.of("17094.67", Currency.USD));
    }

    @Test
    void shouldRejectInvalidRateAndCurrencyPair() {
        assertThatThrownBy(() -> new ExchangeRate(
                Currency.USD, Currency.USD, BigDecimal.ONE, EFFECTIVE_AT, CAPTURED_AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> usdToBrl("0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                BigDecimal.ONE,
                CAPTURED_AT,
                EFFECTIVE_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ExchangeRate usdToBrl(String rate) {
        return new ExchangeRate(
                Currency.USD,
                Currency.BRL,
                new BigDecimal(rate),
                EFFECTIVE_AT,
                CAPTURED_AT);
    }
}
