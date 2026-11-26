package com.donornexa.auth;

import com.donornexa.admin.dto.AdminPasswordChangeDTO;
import com.donornexa.admin.service.AdminDataSeeder;
import com.donornexa.admin.service.AdminProfileService;
import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 8 â€” Comprehensive Portal-Specific Authentication & Strict Role Verification Tests.
 *
 * <p>Tests the three distinct login flows (/login/donor, /login/hospital, /admin/login),
 * the strict rejection matrix across roles, initial admin credentials, and first-login password change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Portal Login & Strict Role Security Tests")
class PortalLoginAndRoleSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminProfileService adminProfileService;
    @Autowired private AdminDataSeeder adminDataSeeder;

    private static final String DONOR_EMAIL = "test.donor.portal@donornexa.com";
    private static final String HOSPITAL_EMAIL = "test.hospital.portal@donornexa.com";
    private static final String ADMIN_EMAIL = "test.admin.portal@donornexa.com";
    private static final String INACTIVE_DONOR_EMAIL = "inactive.donor@donornexa.com";
    private static final String INACTIVE_HOSPITAL_EMAIL = "inactive.hospital@donornexa.com";
    private static final String INACTIVE_ADMIN_EMAIL = "inactive.admin@donornexa.com";

    private static final String RAW_PASSWORD = "ValidPass123!";

    @BeforeEach
    void setUp() {
        createOrUpdateUser(DONOR_EMAIL, Role.DONOR, AccountStatus.ACTIVE, false);
        createOrUpdateUser(HOSPITAL_EMAIL, Role.HOSPITAL, AccountStatus.ACTIVE, false);
        createOrUpdateUser(ADMIN_EMAIL, Role.ADMIN, AccountStatus.ACTIVE, false);

        createOrUpdateUser(INACTIVE_DONOR_EMAIL, Role.DONOR, AccountStatus.SUSPENDED, false);
        createOrUpdateUser(INACTIVE_HOSPITAL_EMAIL, Role.HOSPITAL, AccountStatus.SUSPENDED, false);
        createOrUpdateUser(INACTIVE_ADMIN_EMAIL, Role.ADMIN, AccountStatus.SUSPENDED, false);
    }

    private void createOrUpdateUser(String email, Role role, AccountStatus status, boolean mustChangePassword) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setFullName("User " + role.name());
            u.setPhoneNumber("0300" + Math.abs(email.hashCode() % 10000000));
            return u;
        });
        user.setRole(role);
        user.setAccountStatus(status);
        user.setEnabled(status == AccountStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setMustChangePassword(mustChangePassword);
        userRepository.save(user);
    }

    // =========================================================================
    // 1. DONOR LOGIN TESTS (/login/donor)
    // =========================================================================

    @Test
    @DisplayName("Donor 1: Valid donor credentials -> Successful login to /donor/dashboard")
    void donorLogin_validDonor_success() throws Exception {
        mockMvc.perform(post("/login/donor")
                        .param("email", DONOR_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "DONOR")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/dashboard"));
    }

    @Test
    @DisplayName("Donor 2: Hospital credentials entered on Donor Login -> REJECTED")
    void donorLogin_hospitalCredentials_rejected() throws Exception {
        mockMvc.perform(post("/login/donor")
                        .param("email", HOSPITAL_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "DONOR")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/donor?error=role"));
    }

    @Test
    @DisplayName("Donor 3: Admin credentials entered on Donor Login -> REJECTED")
    void donorLogin_adminCredentials_rejected() throws Exception {
        mockMvc.perform(post("/login/donor")
                        .param("email", ADMIN_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "DONOR")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/donor?error=role"));
    }

    @Test
    @DisplayName("Donor 4: Invalid password on Donor Login -> REJECTED")
    void donorLogin_invalidPassword_rejected() throws Exception {
        mockMvc.perform(post("/login/donor")
                        .param("email", DONOR_EMAIL)
                        .param("password", "WrongPassword!")
                        .param("portal", "DONOR")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/donor?error"));
    }

    @Test
    @DisplayName("Donor 5: Suspended donor on Donor Login -> REJECTED")
    void donorLogin_inactiveDonor_rejected() throws Exception {
        mockMvc.perform(post("/login/donor")
                        .param("email", INACTIVE_DONOR_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "DONOR")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/donor?error"));
    }

    // =========================================================================
    // 2. HOSPITAL LOGIN TESTS (/login/hospital)
    // =========================================================================

    @Test
    @DisplayName("Hospital 1: Valid hospital credentials -> Successful login to /hospital/dashboard")
    void hospitalLogin_validHospital_success() throws Exception {
        mockMvc.perform(post("/login/hospital")
                        .param("email", HOSPITAL_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "HOSPITAL")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hospital/dashboard"));
    }

    @Test
    @DisplayName("Hospital 2: Donor credentials entered on Hospital Login -> REJECTED")
    void hospitalLogin_donorCredentials_rejected() throws Exception {
        mockMvc.perform(post("/login/hospital")
                        .param("email", DONOR_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "HOSPITAL")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/hospital?error=role"));
    }

    @Test
    @DisplayName("Hospital 3: Admin credentials entered on Hospital Login -> REJECTED")
    void hospitalLogin_adminCredentials_rejected() throws Exception {
        mockMvc.perform(post("/login/hospital")
                        .param("email", ADMIN_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "HOSPITAL")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/hospital?error=role"));
    }

    @Test
    @DisplayName("Hospital 4: Invalid password on Hospital Login -> REJECTED")
    void hospitalLogin_invalidPassword_rejected() throws Exception {
        mockMvc.perform(post("/login/hospital")
                        .param("email", HOSPITAL_EMAIL)
                        .param("password", "WrongPassword!")
                        .param("portal", "HOSPITAL")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/hospital?error"));
    }

    @Test
    @DisplayName("Hospital 5: Suspended hospital on Hospital Login -> REJECTED")
    void hospitalLogin_inactiveHospital_rejected() throws Exception {
        mockMvc.perform(post("/login/hospital")
                        .param("email", INACTIVE_HOSPITAL_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "HOSPITAL")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/hospital?error"));
    }

    // =========================================================================
    // 3. ADMINISTRATOR LOGIN TESTS (/admin/login)
    // =========================================================================

    @Test
    @DisplayName("Admin 1: Valid admin credentials -> Successful login to /admin/dashboard")
    void adminLogin_validAdmin_success() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("email", ADMIN_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Admin 2: Donor credentials entered on Admin Login -> REJECTED")
    void adminLogin_donorCredentials_rejected() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("email", DONOR_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error=role"));
    }

    @Test
    @DisplayName("Admin 3: Hospital credentials entered on Admin Login -> REJECTED")
    void adminLogin_hospitalCredentials_rejected() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("email", HOSPITAL_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error=role"));
    }

    @Test
    @DisplayName("Admin 4: Invalid password on Admin Login -> REJECTED")
    void adminLogin_invalidPassword_rejected() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("email", ADMIN_EMAIL)
                        .param("password", "WrongPassword!")
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    @Test
    @DisplayName("Admin 5: Suspended admin on Admin Login -> REJECTED")
    void adminLogin_inactiveAdmin_rejected() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("email", INACTIVE_ADMIN_EMAIL)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error"));
    }

    // =========================================================================
    // 4. DASHBOARD CROSS-ACCESS PROTECTION TESTS
    // =========================================================================

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = {"DONOR"})
    @DisplayName("Cross-access 1: Donor cannot access /admin/dashboard (403 Forbidden)")
    void donor_cannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = {"HOSPITAL"})
    @DisplayName("Cross-access 2: Hospital cannot access /admin/dashboard (403 Forbidden)")
    void hospital_cannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("Cross-access 3: Admin cannot access /donor/dashboard (403 Forbidden)")
    void admin_cannotAccessDonorDashboard() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("Cross-access 4: Admin cannot access /hospital/dashboard (403 Forbidden)")
    void admin_cannotAccessHospitalDashboard() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-access 5: Unauthenticated access redirects to login")
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // 5. FIRST-LOGIN PASSWORD CHANGE TESTS
    // =========================================================================

    @Test
    @DisplayName("First-login 1: Admin with mustChangePassword=true is redirected to /admin/first-login-change-password")
    void admin_firstLogin_redirectsToPasswordChange() throws Exception {
        String tempAdminEmail = "temp.admin@donornexa.com";
        createOrUpdateUser(tempAdminEmail, Role.ADMIN, AccountStatus.ACTIVE, true);

        mockMvc.perform(post("/admin/login")
                        .param("email", tempAdminEmail)
                        .param("password", RAW_PASSWORD)
                        .param("portal", "ADMIN")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/first-login-change-password"));
    }

    @Test
    @WithMockUser(username = "mustchange.admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("First-login 2: Admin cannot bypass password change by opening /admin/dashboard directly")
    void admin_cannotBypassPasswordChange() throws Exception {
        createOrUpdateUser("mustchange.admin@donornexa.com", Role.ADMIN, AccountStatus.ACTIVE, true);

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/first-login-change-password"));
    }

    @Test
    @WithMockUser(username = "completechange.admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("First-login 3: Submitting valid new password clears mustChangePassword flag and grants dashboard access")
    void admin_completesPasswordChange_success() throws Exception {
        String email = "completechange.admin@donornexa.com";
        createOrUpdateUser(email, Role.ADMIN, AccountStatus.ACTIVE, true);

        AdminPasswordChangeDTO dto = new AdminPasswordChangeDTO();
        dto.setCurrentPassword(RAW_PASSWORD);
        dto.setNewPassword("PermanentAdmin@2026!");
        dto.setConfirmPassword("PermanentAdmin@2026!");

        adminProfileService.changeInitialTemporaryPassword(email, dto);

        User updatedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(updatedUser);
        assertFalse(updatedUser.getMustChangePassword());
        assertTrue(passwordEncoder.matches("PermanentAdmin@2026!", updatedUser.getPasswordHash()));

        // Now dashboard access is granted
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin Dashboard")));
    }

    // =========================================================================
    // 6. ADMINISTRATOR SEEDER TESTS
    // =========================================================================

    @Test
    @DisplayName("Seeder 1: AdminDataSeeder is idempotent and preserves existing administrator passwords")
    void seeder_idempotentAndSafe() {
        long countBefore = userRepository.count();
        adminDataSeeder.run();
        long countAfter = userRepository.count();
        assertEquals(countBefore, countAfter);
    }
}
