package com.credit.engine.srm.pricing;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record PricingSimulationCommand(
        ReceivableType receivableType,
        BigDecimal faceValue,
        LocalDate dueDate,
        Currency paymentCurrency) {

    public PricingSimulationCommand {
        Objects.requireNonNull(receivableType, "receivableType is required");
        Objects.requireNonNull(faceValue, "faceValue is required");
        Objects.requireNonNull(dueDate, "dueDate is required");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency is required");
    }
}
