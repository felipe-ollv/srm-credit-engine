package com.credit.engine.srm.reporting.internal.adapter.in.web;

import com.credit.engine.srm.reporting.SettlementStatement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

record SettlementStatementResponseDto(
        UUID settlementId,
        UUID batchId,
        UUID receivableId,
        UUID assignorId,
        String assignorDocument,
        String assignorLegalName,
        String receivableType,
        LocalDate dueDate,
        MoneyResponseDto faceValue,
        MoneyResponseDto presentValue,
        MoneyResponseDto discount,
        MoneyResponseDto payment,
        int termMonths,
        String baseRate,
        String spread,
        ExchangeRateResponseDto exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt,
        Instant settledAt) {

    static SettlementStatementResponseDto from(SettlementStatement statement) {
        return new SettlementStatementResponseDto(
                statement.settlementId(),
                statement.batchId(),
                statement.receivableId(),
                statement.assignorId(),
                statement.assignorDocument(),
                statement.assignorLegalName(),
                statement.receivableType().name(),
                statement.dueDate(),
                MoneyResponseDto.from(statement.faceValue()),
                MoneyResponseDto.from(statement.presentValue()),
                MoneyResponseDto.from(statement.discount()),
                MoneyResponseDto.from(statement.payment()),
                statement.termMonths(),
                decimal(statement.baseRate()),
                decimal(statement.spread()),
                ExchangeRateResponseDto.from(statement.exchangeRate()),
                statement.pricingDate(),
                statement.calculatedAt(),
                statement.settledAt());
    }

    private static String decimal(java.math.BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    record MoneyResponseDto(String amount, String currency) {

        static MoneyResponseDto from(SettlementStatement.MoneySnapshot money) {
            return new MoneyResponseDto(money.amount().toPlainString(), money.currency().name());
        }
    }

    record ExchangeRateResponseDto(
            String baseCurrency,
            String quoteCurrency,
            String rate,
            Instant effectiveAt,
            Instant capturedAt) {

        static ExchangeRateResponseDto from(SettlementStatement.ExchangeRateSnapshot exchangeRate) {
            if (exchangeRate == null) {
                return null;
            }
            return new ExchangeRateResponseDto(
                    exchangeRate.baseCurrency().name(),
                    exchangeRate.quoteCurrency().name(),
                    decimal(exchangeRate.rate()),
                    exchangeRate.effectiveAt(),
                    exchangeRate.capturedAt());
        }
    }
}
