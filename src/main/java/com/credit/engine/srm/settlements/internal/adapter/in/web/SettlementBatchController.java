package com.credit.engine.srm.settlements.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblemResponseDto;
import com.credit.engine.srm.settlements.CreateSettlementBatchUseCase;
import com.credit.engine.srm.settlements.SettlementBatchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/settlement-batches", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Settlement Batches")
class SettlementBatchController {

    private final CreateSettlementBatchUseCase useCase;

    SettlementBatchController(CreateSettlementBatchUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Settle a batch of receivables",
            description = "Processes 1 to 100 items independently and persists the complete idempotent response.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @RequestBody(content = @Content(examples = @ExampleObject(value = """
                    {"items":[{"receivableId":"00000000-0000-0000-0000-000000000001","paymentCurrency":"BRL"}]}
                    """))))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch completed with one result per item",
                    headers = @Header(name = "X-Correlation-Id")),
            @ApiResponse(responseCode = "400", description = "Malformed request",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Invalid batch envelope",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    SettlementBatchResult create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @org.springframework.web.bind.annotation.RequestBody CreateSettlementBatchRequestDto request) {
        return useCase.create(idempotencyKey, request.toCommand());
    }
}
