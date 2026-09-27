package com.ridelink.accountservice.service;

import com.ridelink.accountservice.dto.LoginRequest;
import com.ridelink.accountservice.dto.RegisterRequest;
import com.ridelink.accountservice.dto.TokenResponse;
import com.ridelink.accountservice.exception.AccountNotFoundException;
import com.ridelink.accountservice.exception.DuplicateAccountException;
import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.repository.AccountRepository;
import com.ridelink.accountservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = Account.builder()
                .id("acc-001")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .phone("+94771234567")
                .passwordHash("$2a$12$hash")
                .roles(Set.of("PASSENGER"))
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    // --- Registration tests ---

    @Test
    @DisplayName("register: success with valid passenger request")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("alice@example.com");
        req.setPhone("+94771234567");
        req.setPassword("password123");
        req.setRole("PASSENGER");

        when(accountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(accountRepository.existsByPhone("+94771234567")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hash");
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        var result = accountService.register(req);

        assertThat(result.getId()).isEqualTo("acc-001");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getRoles()).contains("PASSENGER");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("register: throws DuplicateAccountException for duplicate email")
    void register_duplicateEmail_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com");
        req.setPhone("+94771234567");
        req.setRole("PASSENGER");

        when(accountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> accountService.register(req))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("Email is already registered");

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws DuplicateAccountException for duplicate phone")
    void register_duplicatePhone_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("unique@example.com");
        req.setPhone("+94771234567");
        req.setRole("PASSENGER");

        when(accountRepository.existsByEmail("unique@example.com")).thenReturn(false);
        when(accountRepository.existsByPhone("+94771234567")).thenReturn(true);

        assertThatThrownBy(() -> accountService.register(req))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessageContaining("Phone number is already registered");

        verify(accountRepository, never()).save(any());
    }

    // --- Login tests ---

    @Test
    @DisplayName("login: success with valid credentials")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(accountRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleAccount));
        when(jwtTokenProvider.generateToken("acc-001", Set.of("PASSENGER"))).thenReturn("mock.jwt.token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        TokenResponse res = accountService.login(req);

        assertThat(res.getToken()).isEqualTo("mock.jwt.token");
        assertThat(res.getTokenType()).isEqualTo("Bearer");
        assertThat(res.getAccountId()).isEqualTo("acc-001");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: throws BadCredentialsException for invalid password")
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> accountService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
