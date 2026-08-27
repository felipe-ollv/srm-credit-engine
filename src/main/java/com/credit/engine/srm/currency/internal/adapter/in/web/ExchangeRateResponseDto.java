package com.credit.engine.srm.currency.internal.adapter.in.web;

import com.credit.engine.srm.shared.ExchangeRate;

import java.time.Instant;

record ExchangeRateResponseDto(
        String baseCurrency,
        String quoteCurrency,
        String rate,
        Instant effectiveAt,
        Instant capturedAt) {

    static ExchangeRateResponseDto from(ExchangeRate exchangeRate) {
        return new ExchangeRateResponseDto(
                exchangeRate.baseCurrency().name(),
                exchangeRate.quoteCurrency().name(),
                exchangeRate.rate().toPlainString(),
                exchangeRate.effectiveAt(),
                exchangeRate.capturedAt());
    }
}
