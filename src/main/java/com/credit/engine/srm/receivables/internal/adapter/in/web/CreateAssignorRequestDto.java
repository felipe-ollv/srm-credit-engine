package com.credit.engine.srm.receivables.internal.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record CreateAssignorRequestDto(
        @NotBlank
        @Pattern(regexp = "^(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$")
        @Schema(example = "12.345.678/0001-95")
        String document,

        @NotBlank
        @Size(min = 2, max = 160)
        @Schema(example = "Indústria Exemplo S.A.")
        String legalName) {
}
