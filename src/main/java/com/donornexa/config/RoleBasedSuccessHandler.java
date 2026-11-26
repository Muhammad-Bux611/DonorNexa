package com.donornexa.config;

import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.User;
import com.donornexa.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles post-authentication redirection and enforces strict role validation.
 *
 * <p>Validates that the authenticated user's actual database role matches the portal
 * through which login was attempted:
 * <ul>
 *   <li>/login/donor â†’ requires ROLE_DONOR</li>
 *   <li>/login/hospital â†’ requires ROLE_HOSPITAL</li>
 *   <li>/admin/login â†’ requires ROLE_ADMIN</li>
 * </ul>
 * If an account attempts to log in through the wrong portal, authentication is rejected,
 * the session is immediately invalidated, and the user is redirected back with an error.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        Role targetPortal = resolveTargetPortal(request, authentication);

        boolean hasAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean hasHospital = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_HOSPITAL".equals(a.getAuthority()));
        boolean hasDonor = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DONOR".equals(a.getAuthority()));

        boolean roleMatches = false;
        if (targetPortal == Role.ADMIN && hasAdmin) {
            roleMatches = true;
        } else if (targetPortal == Role.HOSPITAL && hasHospital) {
            roleMatches = true;
        } else if (targetPortal == Role.DONOR && hasDonor) {
            roleMatches = true;
        }

        if (!roleMatches) {
            log.warn("Access Denied: User '{}' with authorities {} attempted to log into {} portal",
                    authentication.getName(), authentication.getAuthorities(), targetPortal);

            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            String redirectUrl;
            if (targetPortal == Role.ADMIN) {
                redirectUrl = "/admin/login?error=role";
            } else if (targetPortal == Role.HOSPITAL) {
                redirectUrl = "/login/hospital?error=role";
            } else {
                redirectUrl = "/login/donor?error=role";
            }

            response.sendRedirect(request.getContextPath() + redirectUrl);
            return;
        }

        // Role matches portal - proceed with redirection
        if (targetPortal == Role.ADMIN) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getMustChangePassword())) {
                response.sendRedirect(request.getContextPath() + "/admin/first-login-change-password");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
        } else if (targetPortal == Role.HOSPITAL) {
            response.sendRedirect(request.getContextPath() + "/hospital/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/donor/dashboard");
        }
    }

    private Role resolveTargetPortal(HttpServletRequest request, Authentication authentication) {
        String uri = request.getRequestURI();
        String portalParam = request.getParameter("portal");
        String referer = request.getHeader("Referer");

        if (uri.contains("/admin/login")
                || "ADMIN".equalsIgnoreCase(portalParam)
                || (referer != null && referer.contains("/admin/login"))) {
            return Role.ADMIN;
        }
        if (uri.contains("/login/hospital")
                || "HOSPITAL".equalsIgnoreCase(portalParam)
                || (referer != null && referer.contains("/login/hospital"))) {
            return Role.HOSPITAL;
        }
        if (uri.contains("/login/donor")
                || "DONOR".equalsIgnoreCase(portalParam)
                || (referer != null && referer.contains("/login/donor"))) {
            return Role.DONOR;
        }

        // Legacy / default fallback: match actual role
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) return Role.ADMIN;
            if ("ROLE_HOSPITAL".equals(authority.getAuthority())) return Role.HOSPITAL;
            if ("ROLE_DONOR".equals(authority.getAuthority())) return Role.DONOR;
        }
        return Role.DONOR;
    }
}
