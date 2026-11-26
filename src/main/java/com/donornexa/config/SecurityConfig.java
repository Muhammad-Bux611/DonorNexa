package com.donornexa.config;

import com.donornexa.auth.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Spring Security configuration for DonorNexa.
 *
 * <p>Supports three strictly separated role login flows:
 * <ul>
 *   <li>/login/donor → restricted to ROLE_DONOR</li>
 *   <li>/login/hospital → restricted to ROLE_HOSPITAL</li>
 *   <li>/admin/login → restricted to ROLE_ADMIN</li>
 * </ul>
 *
 * <p>Role-based access:
 * <ul>
 *   <li>Public: home, login flows, registration, static resources</li>
 *   <li>ROLE_DONOR → /donor/**</li>
 *   <li>ROLE_HOSPITAL → /hospital/**</li>
 *   <li>ROLE_ADMIN → /admin/**</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final RoleBasedSuccessHandler  successHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          RoleBasedSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler     = successHandler;
    }

    // -------------------------------------------------------------------------
    // Security filter chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager) throws Exception {

        // Custom authentication filter accepting logins across /login, /login/donor, /login/hospital, /admin/login
        UsernamePasswordAuthenticationFilter portalAuthFilter = new UsernamePasswordAuthenticationFilter(authenticationManager);
        portalAuthFilter.setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/login", "POST"),
                new AntPathRequestMatcher("/login/donor", "POST"),
                new AntPathRequestMatcher("/login/hospital", "POST"),
                new AntPathRequestMatcher("/admin/login", "POST")
        ));
        portalAuthFilter.setUsernameParameter("email");
        portalAuthFilter.setPasswordParameter("password");
        portalAuthFilter.setAuthenticationSuccessHandler(successHandler);
        portalAuthFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
        portalAuthFilter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());

        http
            // -----------------------------------------------------------------
            // Authorization rules
            // -----------------------------------------------------------------
            .authorizeHttpRequests(auth -> auth
                // Public pages — no authentication required
                .requestMatchers(
                        "/",
                        "/about",
                        "/emergencies",
                        "/register",
                        "/register/donor",
                        "/donor-register",
                        "/register/hospital",
                        "/hospital-register",
                        "/login",
                        "/login/donor",
                        "/login/hospital",
                        "/admin/login",
                        "/api/cities",
                        "/api/locations/**",
                        "/api/public/**",
                        "/uploads/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/actuator/health"
                ).permitAll()
                // Role-restricted areas
                .requestMatchers("/admin/first-login-change-password").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/hospital/**").hasRole("HOSPITAL")
                .requestMatchers("/donor/**").hasRole("DONOR")
                // Authenticated notifications center
                .requestMatchers("/notifications/**").authenticated()
                // Any other request requires authentication
                .anyRequest().authenticated()
            )

            // -----------------------------------------------------------------
            // Form login defaults & custom portal filter
            // -----------------------------------------------------------------
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .addFilterBefore(portalAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // -----------------------------------------------------------------
            // Remember-Me Authentication
            // -----------------------------------------------------------------
            .rememberMe(remember -> remember
                .key("DonorNexaSecretRememberMeKey2026")
                .tokenValiditySeconds(86400 * 14) // 14 days
                .userDetailsService(userDetailsService)
                .rememberMeParameter("remember-me")
                .useSecureCookie(false)
            )

            // -----------------------------------------------------------------
            // Logout
            // -----------------------------------------------------------------
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?loggedOut")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )

            // -----------------------------------------------------------------
            // Session management — protect against session fixation
            // -----------------------------------------------------------------
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
            );

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Authentication Manager
    // -------------------------------------------------------------------------

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // -------------------------------------------------------------------------
    // Authentication provider — wires UserDetailsService + PasswordEncoder
    // -------------------------------------------------------------------------

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // -------------------------------------------------------------------------
    // Password encoder
    // -------------------------------------------------------------------------

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // -------------------------------------------------------------------------
    // Failure handler — portal-aware failure redirection
    // -------------------------------------------------------------------------

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String uri = request.getRequestURI();
            String portal = request.getParameter("portal");
            String referer = request.getHeader("Referer");

            String failureUrl = "/login/donor?error";
            if (uri.contains("/admin/login")
                    || "ADMIN".equalsIgnoreCase(portal)
                    || (referer != null && referer.contains("/admin/login"))) {
                failureUrl = "/admin/login?error";
            } else if (uri.contains("/login/hospital")
                    || "HOSPITAL".equalsIgnoreCase(portal)
                    || (referer != null && referer.contains("/login/hospital"))) {
                failureUrl = "/login/hospital?error";
            } else if (uri.contains("/login/donor")
                    || "DONOR".equalsIgnoreCase(portal)
                    || (referer != null && referer.contains("/login/donor"))) {
                failureUrl = "/login/donor?error";
            } else if (uri.equals("/login")) {
                failureUrl = "/login?error";
            }

            response.sendRedirect(request.getContextPath() + failureUrl);
        };
    }
}
