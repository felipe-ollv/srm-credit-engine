package com.credit.engine.srm.pricing;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PricingRequest(
        ReceivableType receivableType,
        Money faceValue,
        LocalDate pricingDate,
        LocalDate dueDate,
        InterestRate baseRate,
        Currency paymentCurrency,
        Optional<ExchangeRate> exchangeRate,
        Instant calculatedAt) {

    public PricingRequest {
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(faceValue, "faceValue is required");
        Objects.requireNonNull(pricingDate, "pricingDate is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
        Objects.requireNonNull(baseRate, "baseRate is required");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency is required");
        exchangeRate = Objects.requireNonNull(exchangeRate, "exchangeRate is required");
        Objects.requireNonNull(calculatedAt, "calculatedAt is required");

        if (faceValue.currency() != Currency.BRL) {
            throw new IllegalArgumentException("faceValue must be denominated in BRL");
        }
        if (!faceValue.isPositive()) {
            throw new IllegalArgumentException("faceValue must be positive");
        }
    }
}
