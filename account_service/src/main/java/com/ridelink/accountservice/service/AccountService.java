package com.ridelink.accountservice.service;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.exception.AccountNotFoundException;
import com.ridelink.accountservice.exception.DuplicateAccountException;
import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.model.Account.AccountStatus;
import com.ridelink.accountservice.repository.AccountRepository;
import com.ridelink.accountservice.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic for account management.
 * Follows Single Responsibility and Dependency Inversion principles.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AccountService(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider,
                          AuthenticationManager authenticationManager) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new passenger or driver account.
     * Validates uniqueness of email and phone before persisting.
     */
    public AccountResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateAccountException("Email is already registered: " + request.getEmail());
        }
        if (accountRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateAccountException("Phone number is already registered: " + request.getPhone());
        }

        Account account = Account.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(request.getRole()))
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Registered new account id={} role={}", saved.getId(), request.getRole());
        return toResponse(saved);
    }

    /**
     * Authenticates a user and returns a JWT token.
     */
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        String token = jwtTokenProvider.generateToken(account.getId(), account.getRoles());
        log.info("Login successful for accountId={}", account.getId());

        return TokenResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(jwtTokenProvider.getExpirationMs())
                .accountId(account.getId())
                .roles(account.getRoles())
                .build();
    }

    /**
     * Retrieves the profile of an account by its ID.
     */
    public AccountResponse getProfile(String accountId) {
        Account account = findById(accountId);
        return toResponse(account);
    }

    /**
     * Partially updates profile fields (firstName, lastName, phone).
     */
    public AccountResponse updateProfile(String accountId, UpdateProfileRequest request) {
        Account account = findById(accountId);

        if (request.getFirstName() != null) account.setFirstName(request.getFirstName());
        if (request.getLastName() != null) account.setLastName(request.getLastName());
        if (request.getPhone() != null) {
            if (!request.getPhone().equals(account.getPhone())
                    && accountRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateAccountException("Phone number already in use: " + request.getPhone());
            }
            account.setPhone(request.getPhone());
        }

        Account saved = accountRepository.save(account);
        log.info("Profile updated for accountId={}", accountId);
        return toResponse(saved);
    }

    /**
     * Suspends an account (admin operation).
     */
    public AccountResponse suspendAccount(String accountId) {
        Account account = findById(accountId);
        account.setStatus(AccountStatus.SUSPENDED);
        return toResponse(accountRepository.save(account));
    }

    /**
     * Reactivates a suspended account (admin operation).
     */
    public AccountResponse activateAccount(String accountId) {
        Account account = findById(accountId);
        account.setStatus(AccountStatus.ACTIVE);
        return toResponse(accountRepository.save(account));
    }

    /**
     * Soft-deletes an account (admin operation).
     */
    public void deleteAccount(String accountId) {
        Account account = findById(accountId);
        account.setStatus(AccountStatus.DELETED);
        accountRepository.save(account);
        log.info("Account soft-deleted: id={}", accountId);
    }

    /**
     * Lists all accounts (admin operation).
     */
    public List<AccountResponse> listAll() {
        return accountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Account findById(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .roles(account.getRoles())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
