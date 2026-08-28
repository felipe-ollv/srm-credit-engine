package com.credit.engine.srm.reporting;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SettlementStatement(
        UUID settlementId,
        UUID batchId,
        UUID receivableId,
        UUID assignorId,
        String assignorDocument,
        String assignorLegalName,
        ReceivableType receivableType,
        LocalDate dueDate,
        MoneySnapshot faceValue,
        MoneySnapshot presentValue,
        MoneySnapshot discount,
        MoneySnapshot payment,
        int termMonths,
        BigDecimal baseRate,
        BigDecimal spread,
        ExchangeRateSnapshot exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt,
        Instant settledAt) {

    public record MoneySnapshot(BigDecimal amount, Currency currency) {
    }

    public record ExchangeRateSnapshot(
            Currency baseCurrency,
            Currency quoteCurrency,
            BigDecimal rate,
            Instant effectiveAt,
            Instant capturedAt) {
    }
}
