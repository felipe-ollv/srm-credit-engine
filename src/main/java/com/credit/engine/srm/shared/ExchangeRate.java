package com.credit.engine.srm.shared;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Objects;

public record ExchangeRate(
        Currency baseCurrency,
        Currency quoteCurrency,
        BigDecimal rate,
        Instant effectiveAt,
        Instant capturedAt) {

    public ExchangeRate {
        Objects.requireNonNull(baseCurrency, "baseCurrency is required");
        Objects.requireNonNull(quoteCurrency, "quoteCurrency is required");
        Objects.requireNonNull(rate, "rate is required");
        Objects.requireNonNull(effectiveAt, "effectiveAt is required");
        Objects.requireNonNull(capturedAt, "capturedAt is required");

        if (baseCurrency == quoteCurrency) {
            throw new IllegalArgumentException("exchange rate currencies must be different");
        }
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("exchange rate must be positive");
        }
        if (capturedAt.isBefore(effectiveAt)) {
            throw new IllegalArgumentException("capturedAt cannot be before effectiveAt");
        }

        rate = rate.stripTrailingZeros();
    }

    public Money convert(Money source, Currency targetCurrency) {
        Objects.requireNonNull(source, "source is required");
        Objects.requireNonNull(targetCurrency, "targetCurrency is required");

        if (source.currency() == targetCurrency) {
            throw new IllegalArgumentException("source and target currencies must be different");
        }

        BigDecimal converted;
        if (source.currency() == baseCurrency && targetCurrency == quoteCurrency) {
            converted = source.amount().multiply(rate, MathContext.DECIMAL128);
        } else if (source.currency() == quoteCurrency && targetCurrency == baseCurrency) {
            converted = source.amount().divide(rate, MathContext.DECIMAL128);
        } else {
            throw new IllegalArgumentException("exchange rate does not support the requested conversion");
        }

        return Money.rounded(converted, targetCurrency);
    }

    public boolean isUsdToBrl() {
        return baseCurrency == Currency.USD && quoteCurrency == Currency.BRL;
    }
}
