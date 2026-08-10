package com.owasp.sales.web;

import com.owasp.sales.dto.ErrorResponse;
import com.owasp.sales.dto.ProductResponse;
import com.owasp.sales.dto.SaleResponse;
import com.owasp.sales.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sales", description = "Product catalog and customer sales records")
@SecurityRequirement(name = "bearerAuth")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping("/get-products")
    @PreAuthorize("hasAnyAuthority('sales-admin', 'sales-user')")
    @Operation(
            summary = "List available products",
            description = "Returns the Example Company rubber duck product catalog. Requires `sales-admin` or `sales-user`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product catalog returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return ResponseEntity.ok(salesService.getProducts());
    }

    @GetMapping("/get-sales")
    @PreAuthorize("hasAnyAuthority('sales-admin', 'sales-user')")
    @Operation(
            summary = "List sales records",
            description = """
                    Returns sales records. Requires `sales-admin` or `sales-user`.
                    `sales-admin` sees full customer PII; `sales-user` sees redacted names and phones (asterisks).
                    Optionally filter by product code.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sales records returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unknown product code",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SaleResponse>> getSales(
            @Parameter(description = "Optional product code filter", example = "CLASSIC_YELLOW")
            @RequestParam(required = false) String product) {
        return ResponseEntity.ok(salesService.getSales(product));
    }
}
