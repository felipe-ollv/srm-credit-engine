package com.credit.engine.srm.currency.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblemResponseDto;
import com.credit.engine.srm.currency.FindCurrentExchangeRateUseCase;
import com.credit.engine.srm.currency.RefreshExchangeRateUseCase;
import com.credit.engine.srm.currency.internal.application.CurrentExchangeRateNotFoundException;
import com.credit.engine.srm.shared.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping(path = "/api/v1/exchange-rates", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Exchange Rates")
class ExchangeRateController {

    private final FindCurrentExchangeRateUseCase findUseCase;
    private final RefreshExchangeRateUseCase refreshUseCase;
    private final Clock clock;

    ExchangeRateController(
            FindCurrentExchangeRateUseCase findUseCase,
            RefreshExchangeRateUseCase refreshUseCase,
            Clock clock) {
        this.findUseCase = findUseCase;
        this.refreshUseCase = refreshUseCase;
        this.clock = clock;
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh the USD/BRL exchange rate",
            description = "Fetches the configured HTTP provider and stores an immutable rate snapshot.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exchange rate captured"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "503", description = "Provider unavailable or invalid",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    ResponseEntity<ExchangeRateResponseDto> refresh() {
        return ResponseEntity.status(201)
                .body(ExchangeRateResponseDto.from(refreshUseCase.refreshUsdToBrl()));
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current exchange rate", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current rate returned"),
            @ApiResponse(responseCode = "400", description = "Invalid currency pair",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "No current rate",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    ExchangeRateResponseDto current(
            @RequestParam Currency baseCurrency,
            @RequestParam Currency quoteCurrency) {
        return findUseCase.find(baseCurrency, quoteCurrency, clock.instant())
                .map(ExchangeRateResponseDto::from)
                .orElseThrow(CurrentExchangeRateNotFoundException::new);
    }
}
