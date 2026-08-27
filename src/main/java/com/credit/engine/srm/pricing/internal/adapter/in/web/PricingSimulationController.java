package com.credit.engine.srm.pricing.internal.adapter.in.web;

import com.credit.engine.srm.pricing.PricingSimulationResult;
import com.credit.engine.srm.pricing.SimulatePricingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(path = "/api/v1/pricing/simulations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pricing")
class PricingSimulationController {

    private final SimulatePricingUseCase useCase;

    PricingSimulationController(SimulatePricingUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase is required");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Simulate receivable pricing",
            description = "Calculates authoritative present value without reserving rates or persisting a settlement.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pricing calculated",
                    content = @Content(schema = @Schema(implementation = PricingSimulationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponseDto.class),
                            examples = @ExampleObject(value = "{\"status\":400,\"code\":\"REQUEST_INVALID\"}"))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Pricing rule violation",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "503", description = "No valid USD/BRL exchange rate",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ApiProblemResponseDto.class),
                            examples = @ExampleObject(value = "{\"code\":\"FX_RATE_UNAVAILABLE\"}")))
    })
    PricingSimulationResponseDto simulate(
            @Valid @RequestBody PricingSimulationRequestDto request) {

        PricingSimulationResult result = useCase.simulate(
                PricingSimulationWebMapper.toCommand(request));
        return PricingSimulationWebMapper.toResponse(result);
    }
}
