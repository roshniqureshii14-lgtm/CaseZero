
package com.casezero.casezero;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CasezeroUserDetailsService implements UserDetailsService {

    private final UserAccountRepository repository;

    public CasezeroUserDetailsService(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        UserAccount account = repository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                    new UsernameNotFoundException("Account not found"));

        return User.withUsername(account.getEmail())
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }
}
