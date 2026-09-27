package com.ridelink.accountservice.controller;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Account Service endpoints.
 * Exposes registration, authentication, and profile management.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ------------------------------------------------------------------
    // Public endpoints (no auth required)
    // ------------------------------------------------------------------

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }

    // ------------------------------------------------------------------
    // Authenticated endpoints (any valid role)
    // ------------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyProfile(Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(accountService.getProfile(accountId));
    }

    @PatchMapping("/me")
    public ResponseEntity<AccountResponse> updateMyProfile(@RequestBody @Valid UpdateProfileRequest request,
                                                            Authentication auth) {
        String accountId = (String) auth.getPrincipal();
        return ResponseEntity.ok(accountService.updateProfile(accountId, request));
    }

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

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAll() {
        return ResponseEntity.ok(accountService.listAll());
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AccountResponse> suspend(@PathVariable String id) {
        return ResponseEntity.ok(accountService.suspendAccount(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AccountResponse> activate(@PathVariable String id) {
        return ResponseEntity.ok(accountService.activateAccount(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
