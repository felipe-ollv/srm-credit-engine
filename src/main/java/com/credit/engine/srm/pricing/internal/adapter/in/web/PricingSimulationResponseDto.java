package com.credit.engine.srm.pricing.internal.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

public record PricingSimulationResponseDto(
        @Schema(example = "DUPLICATA_MERCANTIL") String receivableType,
        MoneyResponseDto faceValue,
        MoneyResponseDto presentValue,
        MoneyResponseDto discount,
        MoneyResponseDto payment,
        @Schema(example = "3") int termMonths,
        @Schema(type = "string", example = "0.01") String baseRate,
        @Schema(type = "string", example = "0.015") String spread,
        @Schema(nullable = true) ExchangeRateResponseDto exchangeRate,
        LocalDate pricingDate,
        Instant calculatedAt) {
}
