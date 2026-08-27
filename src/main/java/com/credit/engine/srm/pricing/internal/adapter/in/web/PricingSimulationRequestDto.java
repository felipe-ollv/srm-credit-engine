package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.ReceivableType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;

public record PricingSimulationRequestDto(
        @NotNull
        @Schema(example = "DUPLICATA_MERCANTIL")
        ReceivableType receivableType,

        @NotBlank
        @Pattern(regexp = "^(?:0|[1-9]\\d{0,16})(?:\\.\\d{1,2})?$")
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @Schema(type = "string", example = "100000.00", pattern = "decimal positivo com até duas casas")
        String faceValue,

        @NotNull
        @Schema(type = "string", format = "date", example = "2026-11-26")
        LocalDate dueDate,

        @NotNull
        @Schema(example = "USD")
        Currency paymentCurrency) {
}
