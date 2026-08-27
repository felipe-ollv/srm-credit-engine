package com.credit.engine.srm.pricing.internal.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.Map;

@Schema(name = "ApiProblem", description = "RFC 9457 Problem Details with API-specific fields")
record ApiProblemResponseDto(
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String title,
        @Schema(example = "Request validation failed") String detail,
        @Schema(example = "/api/v1/pricing/simulations") URI instance,
        @Schema(example = "REQUEST_INVALID") String code,
        @Schema(example = "front-request-123") String correlationId,
        @Schema(description = "Validation messages keyed by request field", nullable = true)
        Map<String, String> fieldErrors) {
}
