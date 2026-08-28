package com.credit.engine.srm.reporting.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblemResponseDto;
import com.credit.engine.srm.reporting.SearchSettlementsUseCase;
import com.credit.engine.srm.reporting.SettlementSearchQuery;
import com.credit.engine.srm.reporting.SettlementSort;
import com.credit.engine.srm.shared.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(path = "/api/v1/settlements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Settlement Reporting")
class SettlementStatementController {

    private final SearchSettlementsUseCase useCase;

    SettlementStatementController(SearchSettlementsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(
            summary = "Search immutable settlement statements",
            description = "Filters by inclusive São Paulo business dates, assignor and payment currency.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, page or sort",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    SettlementPageResponseDto search(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID assignorId,
            @RequestParam(required = false) Currency paymentCurrency,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "settledAt,desc") String sort) {
        return SettlementPageResponseDto.from(useCase.search(new SettlementSearchQuery(
                from,
                to,
                assignorId,
                paymentCurrency,
                page,
                size,
                SettlementSort.parse(sort))));
    }
}
