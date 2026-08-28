package com.credit.engine.srm.settlements;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementResult(
        UUID settlementId,
        UUID receivableId,
        UUID assignorId,
        String assignorDocument,
        String assignorLegalName,
        String receivableType,
        LocalDate dueDate,
        MoneyResult faceValue,
        MoneyResult presentValue,
        MoneyResult discount,
        MoneyResult payment,
        int termMonths,
        String baseRate,
        String spread,
        ExchangeRateResult exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt,
        Instant settledAt) {

    public record MoneyResult(String amount, String currency) {
    }

    public record ExchangeRateResult(
            String baseCurrency,
            String quoteCurrency,
            String rate,
            Instant effectiveAt,
            Instant capturedAt) {
    }
}
