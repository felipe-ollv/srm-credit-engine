package com.credit.engine.srm.pricing;

import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PricingSimulationResult(
        ReceivableType receivableType,
        Money faceValue,
        Money presentValue,
        Money discount,
        Money payment,
        int termMonths,
        InterestRate baseRate,
        InterestRate spread,
        Optional<ExchangeRate> exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt) {

    public PricingSimulationResult {
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(faceValue, "faceValue is required");
        Objects.requireNonNull(presentValue, "presentValue is required");
        Objects.requireNonNull(discount, "discount is required");
        Objects.requireNonNull(payment, "payment is required");
        Objects.requireNonNull(baseRate, "baseRate is required");
        Objects.requireNonNull(spread, "spread is required");
        exchangeRate = Objects.requireNonNull(exchangeRate, "exchangeRate is required");
        Objects.requireNonNull(pricingDate, "pricingDate is required");
        Objects.requireNonNull(calculatedAt, "calculatedAt is required");
    }

    public static PricingSimulationResult from(PricingResult result) {
        Objects.requireNonNull(result, "result is required");
        return new PricingSimulationResult(
                result.receivableType(),
                result.faceValueBrl(),
                result.presentValueBrl(),
                result.discountBrl(),
                result.paymentAmount(),
                result.term().months(),
                result.baseRate(),
                result.spread(),
                result.exchangeRate(),
                result.pricingDate(),
                result.calculatedAt());
    }
}
