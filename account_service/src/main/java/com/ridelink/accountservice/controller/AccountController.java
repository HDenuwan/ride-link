package com.ridelink.accountservice.controller;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Account Service endpoints.
 * Exposes registration, authentication and profile management.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Service", description = "User registration, authentication and profile management")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ------------------------------------------------------------------
    // Public endpoints (no auth required)
    // ------------------------------------------------------------------

    @Operation(summary = "Register a new account", description = "Creates a PASSENGER or DRIVER account.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Email or phone already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request));
    }

    @Operation(summary = "Authenticate and get JWT", description = "Returns a Bearer JWT token on success.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }

    // ------------------------------------------------------------------
    // Authenticated endpoints (any valid role)
    // ------------------------------------------------------------------

    @Operation(summary = "Get own profile", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyProfile(Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(accountService.getProfile(accountId));
    }

    @Operation(summary = "Update own profile", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/me")
    public ResponseEntity<AccountResponse> updateMyProfile(@RequestBody @Valid UpdateProfileRequest request,
                                                            Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(accountService.updateProfile(accountId, request));
    }

    @Operation(summary = "Get profile by ID (self or admin)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getProfile(@PathVariable String id, Authentication auth) {
        String requesterId = (String) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !requesterId.equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(accountService.getProfile(id));
    }

    // ------------------------------------------------------------------
    // Admin-only endpoints
    // ------------------------------------------------------------------

    @Operation(summary = "List all accounts (admin only)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponse>> listAll() {
        return ResponseEntity.ok(accountService.listAll());
    }

    @Operation(summary = "Suspend an account (admin only)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> suspend(@PathVariable String id) {
        return ResponseEntity.ok(accountService.suspendAccount(id));
    }

    @Operation(summary = "Activate an account (admin only)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(accountService.activateAccount(id));
    }

    @Operation(summary = "Delete an account (admin only)",
               security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
