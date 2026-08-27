package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblemResponseDto;
import com.credit.engine.srm.receivables.CreateReceivableCommand;
import com.credit.engine.srm.receivables.CreateReceivableUseCase;
import com.credit.engine.srm.receivables.ReceivableStatusView;
import com.credit.engine.srm.receivables.ReceivableView;
import com.credit.engine.srm.receivables.SearchReceivablesQuery;
import com.credit.engine.srm.receivables.SearchReceivablesUseCase;
import com.credit.engine.srm.shared.AssignorId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(path = "/api/v1/receivables", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Receivables")
class ReceivableController {

    private final CreateReceivableUseCase createUseCase;
    private final SearchReceivablesUseCase searchUseCase;

    ReceivableController(CreateReceivableUseCase createUseCase, SearchReceivablesUseCase searchUseCase) {
        this.createUseCase = createUseCase;
        this.searchUseCase = searchUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a receivable", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Receivable created"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Assignor not found",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Receivable rule violation",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    ResponseEntity<ReceivableResponseDto> create(@Valid @RequestBody CreateReceivableRequestDto request) {
        ReceivableView created = createUseCase.create(new CreateReceivableCommand(
                new AssignorId(request.assignorId()),
                request.type(),
                new BigDecimal(request.faceValue()),
                request.dueDate()));
        return ResponseEntity.created(URI.create("/api/v1/receivables/" + created.id().value()))
                .body(ReceivableResponseDto.from(created));
    }

    @GetMapping
    @Operation(summary = "Search receivables", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receivable page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filters",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    PageResponseDto<ReceivableResponseDto> search(
            @RequestParam(required = false) UUID assignorId,
            @RequestParam(required = false) ReceivableStatusView status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponseDto.from(
                searchUseCase.search(new SearchReceivablesQuery(
                        assignorId == null ? null : new AssignorId(assignorId),
                        status,
                        page,
                        size)),
                ReceivableResponseDto::from);
    }
}
