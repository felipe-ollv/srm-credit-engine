package com.credit.engine.srm.pricing;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PricingResult(
        ReceivableType receivableType,
        Money faceValueBrl,
        Money presentValueBrl,
        Money discountBrl,
        Money paymentAmount,
        Term term,
        InterestRate baseRate,
        InterestRate spread,
        Optional<ExchangeRate> exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt) {

    public PricingResult {
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(faceValueBrl, "faceValueBrl is required");
        Objects.requireNonNull(presentValueBrl, "presentValueBrl is required");
        Objects.requireNonNull(discountBrl, "discountBrl is required");
        Objects.requireNonNull(paymentAmount, "paymentAmount is required");
        Objects.requireNonNull(term, "term is required");
        Objects.requireNonNull(baseRate, "baseRate is required");
        Objects.requireNonNull(spread, "spread is required");
        exchangeRate = Objects.requireNonNull(exchangeRate, "exchangeRate is required");
        Objects.requireNonNull(pricingDate, "pricingDate is required");
        Objects.requireNonNull(calculatedAt, "calculatedAt is required");

        requireBrl(faceValueBrl, "faceValueBrl");
        requireBrl(presentValueBrl, "presentValueBrl");
        requireBrl(discountBrl, "discountBrl");

        if (!faceValueBrl.isPositive() || presentValueBrl.isNegative()) {
            throw new IllegalArgumentException("face value must be positive and present value cannot be negative");
        }
        if (discountBrl.isNegative()) {
            throw new IllegalArgumentException("discount cannot be negative");
        }
        if (!faceValueBrl.subtract(presentValueBrl).equals(discountBrl)) {
            throw new IllegalArgumentException("discount must equal face value minus present value");
        }

        if (paymentAmount.currency() == Currency.BRL) {
            if (exchangeRate.isPresent()) {
                throw new IllegalArgumentException("BRL payment cannot have an exchange rate");
            }
            if (!paymentAmount.equals(presentValueBrl)) {
                throw new IllegalArgumentException("BRL payment must equal BRL present value");
            }
        } else {
            ExchangeRate appliedRate = exchangeRate.orElseThrow(
                    () -> new IllegalArgumentException("USD payment requires an exchange rate"));
            if (!appliedRate.isUsdToBrl()) {
                throw new IllegalArgumentException("USD payment requires an USD/BRL exchange rate");
            }
            if (!appliedRate.convert(presentValueBrl, Currency.USD).equals(paymentAmount)) {
                throw new IllegalArgumentException("USD payment does not match the applied exchange rate");
            }
        }
    }

    private static void requireBrl(Money money, String field) {
        if (money.currency() != Currency.BRL) {
            throw new IllegalArgumentException(field + " must be denominated in BRL");
        }
    }
}
