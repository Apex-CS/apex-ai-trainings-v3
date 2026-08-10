package com.owasp.financial.web;

import com.owasp.financial.dto.BudgetResponse;
import com.owasp.financial.dto.UpdateBudgetRequest;
import com.owasp.financial.security.AuthenticatedUser;
import com.owasp.financial.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Budget", description = "Area budget retrieval and updates")
@SecurityRequirement(name = "bearerAuth")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/get-budget-by-area")
    @PreAuthorize("hasAnyAuthority('financial-admin', 'financial-user')")
    @Operation(
            summary = "Get budget by area",
            description = "Returns quarterly budget records for an area. Requires `financial-admin` or `financial-user`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget records found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No records for the area",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class)))
    })
    public ResponseEntity<List<BudgetResponse>> getBudgetByArea(
            @Parameter(description = "Business area", example = "IT")
            @RequestParam String area,
            @Parameter(description = "Optional fiscal year filter", example = "2026")
            @RequestParam(required = false) Integer fiscalYear) {
        return ResponseEntity.ok(budgetService.getBudgetByArea(area, fiscalYear));
    }

    @PutMapping("/update-budget-by-area")
    @PreAuthorize("hasAuthority('financial-admin')")
    @Operation(
            summary = "Update budget by area",
            description = "Upserts a budget record for an area, quarter, and year. Requires `financial-admin`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget upserted"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = com.owasp.financial.dto.ErrorResponse.class)))
    })
    public ResponseEntity<BudgetResponse> updateBudgetByArea(
            @Valid @RequestBody UpdateBudgetRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(budgetService.upsertBudget(request, user.username()));
    }
}
