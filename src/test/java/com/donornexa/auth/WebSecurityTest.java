package com.donornexa.auth;

import com.donornexa.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3 — Web Layer Security Tests (MockMvc)
 *
 * <p>Tests the Spring Security filter chain, role-based access control,
 * login/logout flows, and CSRF protection.
 * Uses the H2 in-memory database via the "test" profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 3 — Web Security Tests")
class WebSecurityTest {

    @Autowired MockMvc mockMvc;

    // =========================================================================
    // Public pages — accessible without authentication
    // =========================================================================

    @Test
    @DisplayName("Public: home page is accessible without authentication")
    void publicHome_accessible() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public: login page is accessible without authentication")
    void publicLogin_accessible() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public: /register is accessible without authentication")
    void publicRegister_accessible() throws Exception {
        mockMvc.perform(get("/register"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public: donor registration form is accessible without authentication")
    void publicDonorRegisterForm_accessible() throws Exception {
        mockMvc.perform(get("/register/donor"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public: hospital registration form is accessible without authentication")
    void publicHospitalRegisterForm_accessible() throws Exception {
        mockMvc.perform(get("/register/hospital"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public: /api/locations/provinces is accessible without authentication")
    void publicLocationProvinces_accessible() throws Exception {
        mockMvc.perform(get("/api/locations/provinces"))
               .andExpect(status().isOk());
    }

    // =========================================================================
    // Unauthorized access protection — unauthenticated user redirected to login
    // =========================================================================

    @Test
    @DisplayName("Unauthorized: /donor/dashboard redirects to login")
    void donorDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Unauthorized: /hospital/dashboard redirects to login")
    void hospitalDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Unauthorized: /admin/dashboard redirects to login")
    void adminDashboard_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // Donor authorization
    // =========================================================================

    @Test
    @WithMockUser(username = "donor@test.com", roles = "DONOR")
    @DisplayName("Donor: can access /donor/dashboard")
    void donorRole_canAccessDonorDashboard() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "donor@test.com", roles = "DONOR")
    @DisplayName("Donor: forbidden from /hospital/dashboard (403)")
    void donorRole_forbiddenFromHospitalDashboard() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "donor@test.com", roles = "DONOR")
    @DisplayName("Donor: forbidden from /admin/dashboard (403)")
    void donorRole_forbiddenFromAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Hospital authorization
    // =========================================================================

    @Test
    @WithMockUser(username = "hosp@test.com", roles = "HOSPITAL")
    @DisplayName("Hospital: can access /hospital/dashboard")
    void hospitalRole_canAccessHospitalDashboard() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "hosp@test.com", roles = "HOSPITAL")
    @DisplayName("Hospital: forbidden from /donor/dashboard (403)")
    void hospitalRole_forbiddenFromDonorDashboard() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hosp@test.com", roles = "HOSPITAL")
    @DisplayName("Hospital: forbidden from /admin/dashboard (403)")
    void hospitalRole_forbiddenFromAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Admin authorization
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Admin: can access /admin/dashboard")
    void adminRole_canAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Admin: forbidden from /donor/dashboard (403)")
    void adminRole_forbiddenFromDonorDashboard() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
               .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Login failure
    // =========================================================================

    @Test
    @DisplayName("Login: bad credentials redirects to /login?error")
    void login_badCredentials_redirectsToLoginError() throws Exception {
        mockMvc.perform(post("/login")
                .param("email", "nobody@test.com")
                .param("password", "wrongpassword")
                .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/login?error"));
    }

    // =========================================================================
    // Logout
    // =========================================================================

    @Test
    @WithMockUser(username = "user@test.com", roles = "DONOR")
    @DisplayName("Logout: invalidates session and redirects")
    void logout_success() throws Exception {
        mockMvc.perform(logout())
               .andExpect(status().is3xxRedirection());
    }
}
