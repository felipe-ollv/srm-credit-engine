package com.credit.engine.srm.pricing;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ExchangeRate;
import com.credit.engine.srm.shared.Money;
import com.credit.engine.srm.shared.ReceivableType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PriceReceivableCommand(
        ReceivableType receivableType,
        Money faceValue,
        LocalDate dueDate,
        Currency paymentCurrency,
        Instant calculatedAt,
        Optional<ExchangeRate> exchangeRate) {

    public PriceReceivableCommand {
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(faceValue, "faceValue is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency is required");
        Objects.requireNonNull(calculatedAt, "calculatedAt is required");
        exchangeRate = Objects.requireNonNull(exchangeRate, "exchangeRate is required");
    }
}
