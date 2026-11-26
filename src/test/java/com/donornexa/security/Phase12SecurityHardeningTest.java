package com.donornexa.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 12 - Security Hardening & Authorization Matrix Automated Tests.
 *
 * <p>Verifies defense-in-depth:
 * 1. BCrypt password encryption strength & salting.
 * 2. Strict portal authentication boundaries and redirects for unauthenticated visitors.
 * 3. Strict 403 Forbidden enforcement across role boundaries (Admin vs Hospital vs Donor).
 * 4. CSRF protection on POST actions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 12: Security Hardening & Access Control Tests")
class Phase12SecurityHardeningTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Security: Passwords must be hashed using BCrypt with salt")
    void passwordEncoder_shouldBeBCryptAndSecure() {
        String rawPassword = "SecurePassword123!";
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // BCrypt starts with $2a$ or $2b$
        assertTrue(encoded1.startsWith("$2a$") || encoded1.startsWith("$2b$"),
                "Password hash must be standard BCrypt format");
        // Salting ensures identical passwords have distinct hashes
        assertFalse(encoded1.equals(encoded2),
                "BCrypt must use random salts for each hashing invocation");
        assertTrue(passwordEncoder.matches(rawPassword, encoded1),
                "BCrypt must successfully verify matching password");
        assertFalse(passwordEncoder.matches("WrongPassword", encoded1),
                "BCrypt must reject non-matching password");
    }

    @Test
    @DisplayName("Security: Unauthenticated request to /admin/dashboard redirects to login")
    void unauthenticatedAccessToAdmin_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    @Test
    @DisplayName("Security: Unauthenticated request to /hospital/dashboard redirects to login")
    void unauthenticatedAccessToHospital_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    @Test
    @DisplayName("Security: Unauthenticated request to /donor/dashboard redirects to login")
    void unauthenticatedAccessToDonor_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login*"));
    }

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = {"DONOR"})
    @DisplayName("Security: Donor role attempting to access /admin/dashboard receives 403 Forbidden")
    void donorRoleAccessingAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = {"DONOR"})
    @DisplayName("Security: Donor role attempting to access /hospital/dashboard receives 403 Forbidden")
    void donorRoleAccessingHospital_isForbidden() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hospital@donornexa.com", roles = {"HOSPITAL"})
    @DisplayName("Security: Hospital role attempting to access /admin/dashboard receives 403 Forbidden")
    void hospitalRoleAccessingAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hospital@donornexa.com", roles = {"HOSPITAL"})
    @DisplayName("Security: Hospital role attempting to access /donor/dashboard receives 403 Forbidden")
    void hospitalRoleAccessingDonor_isForbidden() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("Security: Admin role can successfully access /admin/dashboard")
    void adminRoleAccessingAdmin_isOk() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: POST request without CSRF token is forbidden (403)")
    void postWithoutCsrfToken_isForbidden() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: POST request with CSRF token is processed (302 redirect after logout)")
    void postWithCsrfToken_isProcessed() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
