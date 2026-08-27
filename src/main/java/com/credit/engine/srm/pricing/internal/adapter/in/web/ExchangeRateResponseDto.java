package com.credit.engine.srm.pricing.internal.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ExchangeRateResponseDto(
        @Schema(example = "USD") String baseCurrency,
        @Schema(example = "BRL") String quoteCurrency,
        @Schema(type = "string", example = "5.4321") String rate,
        Instant effectiveAt,
        Instant capturedAt) {
}
