package com.donornexa.config;

import com.donornexa.user.entity.User;
import com.donornexa.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor ensuring that administrators with temporary passwords must update
 * their credentials before accessing any administrative dashboard resources.
 */
@Component
@RequiredArgsConstructor
public class AdminPasswordChangeInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (isAdmin) {
                String uri = request.getRequestURI();
                if (uri.contains("/admin/first-login-change-password")
                        || uri.contains("/admin/login")
                        || uri.contains("/logout")
                        || uri.contains("/css")
                        || uri.contains("/js")
                        || uri.contains("/images")) {
                    return true;
                }

                User user = userRepository.findByEmail(auth.getName()).orElse(null);
                if (user != null && Boolean.TRUE.equals(user.getMustChangePassword())) {
                    response.sendRedirect(request.getContextPath() + "/admin/first-login-change-password");
                    return false;
                }
            }
        }
        return true;
    }
}
