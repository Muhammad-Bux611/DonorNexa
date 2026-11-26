package com.donornexa.auth.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves login views for general users and the dedicated administrator portal.
 * Actual authentication (POST /login) is handled by Spring Security.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return redirectToDashboard(authentication);
        }
        return "auth/login";
    }

    @GetMapping("/login/donor")
    public String donorLoginPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return redirectToDashboard(authentication);
        }
        return "auth/donor-login";
    }

    @GetMapping("/login/hospital")
    public String hospitalLoginPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return redirectToDashboard(authentication);
        }
        return "auth/hospital-login";
    }

    @GetMapping("/admin/login")
    public String adminLoginPage(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                    return "redirect:/admin/dashboard";
                }
            }
            return redirectToDashboard(authentication);
        }
        return "admin/login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private String redirectToDashboard(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_HOSPITAL".equals(role)) {
                return "redirect:/hospital/dashboard";
            } else if ("ROLE_DONOR".equals(role)) {
                return "redirect:/donor/dashboard";
            }
        }
        return "redirect:/";
    }
}
