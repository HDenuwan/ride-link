package com.ridelink.accountservice.security;

import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.repository.AccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Loads UserDetails from MongoDB for Spring Security authentication.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found for email: " + email));

        if (account.getStatus() == Account.AccountStatus.SUSPENDED) {
            throw new UsernameNotFoundException("Account is suspended");
        }
        if (account.getStatus() == Account.AccountStatus.DELETED) {
            throw new UsernameNotFoundException("Account has been deleted");
        }

        var authorities = account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return User.builder()
                .username(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
