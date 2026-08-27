package com.credit.engine.srm.pricing.internal.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record MoneyResponseDto(
        @Schema(type = "string", example = "92859.94") String amount,
        @Schema(example = "BRL") String currency) {
}
