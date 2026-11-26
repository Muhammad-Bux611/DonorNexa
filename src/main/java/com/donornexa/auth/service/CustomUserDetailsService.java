package com.donornexa.auth.service;

import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security UserDetailsService implementation.
 *
 * <p>Loads a {@link User} by email address and converts it into Spring Security
 * {@link UserDetails}.  The "username" in Spring Security terms is the user's
 * email address throughout this application.
 *
 * <p>Accounts that are INACTIVE or SUSPENDED are reported as disabled so Spring
 * Security refuses the login before the password is ever checked.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for email: " + email));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        boolean accountEnabled = isAccountEnabled(user);

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Use the full 7-argument constructor provided by Spring Security's User class.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                accountEnabled,  // enabled
                true,            // accountNonExpired
                true,            // credentialsNonExpired
                true,            // accountNonLocked
                authorities
        );
    }

    /**
     * An account is considered login-eligible when it is ACTIVE or
     * PENDING_VERIFICATION.  INACTIVE and SUSPENDED accounts are disabled.
     */
    private boolean isAccountEnabled(User user) {
        AccountStatus status = user.getAccountStatus();
        return status == AccountStatus.ACTIVE
                || status == AccountStatus.PENDING_VERIFICATION;
    }
}
