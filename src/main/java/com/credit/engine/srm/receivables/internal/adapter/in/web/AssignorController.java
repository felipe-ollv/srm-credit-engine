package com.credit.engine.srm.receivables.internal.adapter.in.web;

import com.credit.engine.srm.config.web.ApiProblemResponseDto;
import com.credit.engine.srm.receivables.AssignorView;
import com.credit.engine.srm.receivables.CreateAssignorCommand;
import com.credit.engine.srm.receivables.CreateAssignorUseCase;
import com.credit.engine.srm.receivables.SearchAssignorsQuery;
import com.credit.engine.srm.receivables.SearchAssignorsUseCase;
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
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Validated
@RestController
@RequestMapping(path = "/api/v1/assignors", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Assignors")
class AssignorController {

    private final CreateAssignorUseCase createUseCase;
    private final SearchAssignorsUseCase searchUseCase;

    AssignorController(CreateAssignorUseCase createUseCase, SearchAssignorsUseCase searchUseCase) {
        this.createUseCase = createUseCase;
        this.searchUseCase = searchUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an assignor", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assignor created"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Document already registered",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Assignor rule violation",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    ResponseEntity<AssignorResponseDto> create(@Valid @RequestBody CreateAssignorRequestDto request) {
        AssignorView created = createUseCase.create(
                new CreateAssignorCommand(request.document(), request.legalName()));
        return ResponseEntity.created(URI.create("/api/v1/assignors/" + created.id().value()))
                .body(AssignorResponseDto.from(created));
    }

    @GetMapping
    @Operation(summary = "Search assignors", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignor page returned"),
            @ApiResponse(responseCode = "400", description = "Invalid filters",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiProblemResponseDto.class)))
    })
    PageResponseDto<AssignorResponseDto> search(
            @RequestParam(required = false) @Size(max = 160) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponseDto.from(
                searchUseCase.search(new SearchAssignorsQuery(query, page, size)),
                AssignorResponseDto::from);
    }
}
