package com.owasp.it.web;

import com.owasp.it.dto.AppRestartResponse;
import com.owasp.it.dto.AppServerResponse;
import com.owasp.it.dto.ErrorResponse;
import com.owasp.it.dto.RestartServerRequest;
import com.owasp.it.security.AuthenticatedUser;
import com.owasp.it.security.InvalidTokenException;
import com.owasp.it.service.AppServerNotFoundException;
import com.owasp.it.service.ItOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "IT Operations", description = "Application server catalog and restart management")
@SecurityRequirement(name = "bearerAuth")
public class ItOperationsController {

    private final ItOperationsService itOperationsService;

    public ItOperationsController(ItOperationsService itOperationsService) {
        this.itOperationsService = itOperationsService;
    }

    @GetMapping("/list-app-servers")
    @PreAuthorize("hasAnyAuthority('it-admin', 'it-user')")
    @Operation(summary = "List application servers", description = "Requires `it-admin` or `it-user`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Server catalog returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AppServerResponse>> listAppServers() {
        return ResponseEntity.ok(itOperationsService.listAppServers());
    }

    @PostMapping("/restart-server")
    @PreAuthorize("hasAuthority('it-admin')")
    @Operation(summary = "Restart an application server", description = "Requires `it-admin`. Records restart attempt in `app_restarts`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restart attempt recorded"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unknown app server",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AppRestartResponse> restartServer(
            @Valid @RequestBody RestartServerRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(itOperationsService.restartServer(request, user.username()));
    }

    @GetMapping("/list-app-restarts-by-app")
    @PreAuthorize("hasAuthority('it-admin')")
    @Operation(summary = "List restart attempts for an app", description = "Requires `it-admin`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restart history returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unknown app server",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AppRestartResponse>> listAppRestartsByApp(
            @Parameter(description = "Application name", example = "financial-backend")
            @RequestParam String appName) {
        return ResponseEntity.ok(itOperationsService.listAppRestartsByApp(appName));
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(AppServerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAppNotFound(AppServerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("not_found", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(ErrorResponse.of("validation_error", message));
    }
}
