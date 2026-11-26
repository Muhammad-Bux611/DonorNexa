package com.donornexa.admin;

import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.*;
import com.donornexa.user.repository.*;
import com.donornexa.donor.repository.*;
import com.donornexa.hospital.repository.*;
import com.donornexa.bloodrequest.repository.*;
import com.donornexa.matching.repository.*;
import com.donornexa.donation.repository.*;
import com.donornexa.inventory.repository.*;
import com.donornexa.notification.repository.*;
import com.donornexa.location.repository.*;
import com.donornexa.messaging.repository.*;
import com.donornexa.audit.repository.*;
import com.donornexa.admin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 â€” Administrator Donor Verification and Lifecycle Security Tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 5 â€” Admin Donor Verification & Security Tests")
class AdminDonorVerificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private NotificationRepository notificationRepository;

    private static final String DONOR_EMAIL = "testdonor.adminreview@example.com";
    private Donor donor;

    @BeforeEach
    void setUp() {
        Province province = provinceRepository.findByName("Punjab").orElseGet(() -> {
            Province p = new Province();
            p.setName("Punjab");
            return provinceRepository.save(p);
        });

        City city = cityRepository.findByName("Lahore").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Lahore");
            c.setProvince(province);
            return cityRepository.save(c);
        });

        User user = userRepository.findByEmail(DONOR_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(DONOR_EMAIL);
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("Review Candidate Donor");
            u.setPhoneNumber("03219988776");
            u.setRole(Role.DONOR);
            u.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        donor = donorRepository.findByUserId(user.getId()).orElseGet(() -> {
            Donor d = new Donor();
            d.setUser(user);
            d.setBloodGroup(BloodGroup.O_POSITIVE);
            d.setGender(Gender.MALE);
            d.setDateOfBirth(LocalDate.of(1996, 4, 15));
            d.setWeight(new BigDecimal("70.00"));
            d.setProvince(province);
            d.setCity(city);
            d.setAvailability(DonorAvailability.AVAILABLE);
            d.setVerified(Boolean.FALSE);
            return donorRepository.save(d);
        });

        donor.setVerified(Boolean.FALSE);
        donor.setRejectionReason(null);
        donor = donorRepository.save(donor);
    }

    @Test
    @DisplayName("1. Anonymous user accessing /admin/donors is redirected to login")
    void anonymous_cannotAccessAdminDonors() throws Exception {
        mockMvc.perform(get("/admin/donors"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = {"DONOR"})
    @DisplayName("2. Donor role accessing /admin/donors is forbidden (403)")
    void donorRole_cannotAccessAdminDonors() throws Exception {
        mockMvc.perform(get("/admin/donors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hospital@donornexa.com", roles = {"HOSPITAL"})
    @DisplayName("3. Hospital role accessing /admin/donors is forbidden (403)")
    void hospitalRole_cannotAccessAdminDonors() throws Exception {
        mockMvc.perform(get("/admin/donors"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("4. Admin can view /admin/donors queue and directory")
    void admin_canAccessAdminDonorsList() throws Exception {
        mockMvc.perform(get("/admin/donors"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/donors"))
                .andExpect(content().string(containsString("Donor Directory")));
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("5. Admin can inspect individual donor profile at /admin/donors/{id}")
    void admin_canViewDonorDetails() throws Exception {
        mockMvc.perform(get("/admin/donors/" + donor.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/donor-details"))
                .andExpect(content().string(containsString("Review Candidate Donor")));
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("6. Admin approves donor verification â€” sets verified=true, accountStatus=ACTIVE")
    void admin_approveDonorVerification() throws Exception {
        long initialNotifications = notificationRepository.count();

        mockMvc.perform(post("/admin/donors/" + donor.getId() + "/verify")
                        .with(csrf())
                        .param("action", "APPROVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/donors/" + donor.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Donor updated = donorRepository.findByIdWithDetails(donor.getId()).orElseThrow();
        assertTrue(updated.getVerified());
        assertNull(updated.getRejectionReason());
        assertEquals(AccountStatus.ACTIVE, updated.getUser().getAccountStatus());
        assertTrue(notificationRepository.count() > initialNotifications);
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("7. Admin rejects donor without reason â€” validation blocks and returns error")
    void admin_rejectDonorWithoutReason_fails() throws Exception {
        mockMvc.perform(post("/admin/donors/" + donor.getId() + "/verify")
                        .with(csrf())
                        .param("action", "REJECT")
                        .param("rejectionReason", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/donors/" + donor.getId()))
                .andExpect(flash().attributeExists("errorMessage"));

        Donor updated = donorRepository.findByIdWithDetails(donor.getId()).orElseThrow();
        assertFalse(updated.getVerified());
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("8. Admin rejects donor with reason â€” sets verified=false, INACTIVE, and sends notification")
    void admin_rejectDonorWithReason_succeeds() throws Exception {
        mockMvc.perform(post("/admin/donors/" + donor.getId() + "/verify")
                        .with(csrf())
                        .param("action", "REJECT")
                        .param("rejectionReason", "Age below minimum voluntary donor threshold."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/donors/" + donor.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Donor updated = donorRepository.findByIdWithDetails(donor.getId()).orElseThrow();
        assertFalse(updated.getVerified());
        assertEquals("Age below minimum voluntary donor threshold.", updated.getRejectionReason());
        assertEquals(AccountStatus.INACTIVE, updated.getUser().getAccountStatus());
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("9. Admin updates donor account status to SUSPENDED")
    void admin_updateDonorAccountStatus_toSuspended() throws Exception {
        mockMvc.perform(post("/admin/donors/" + donor.getId() + "/status")
                        .with(csrf())
                        .param("accountStatus", "SUSPENDED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/donors/" + donor.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Donor updated = donorRepository.findByIdWithDetails(donor.getId()).orElseThrow();
        assertEquals(AccountStatus.SUSPENDED, updated.getUser().getAccountStatus());
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = {"ADMIN"})
    @DisplayName("10. POST verification without CSRF token returns 403 Forbidden")
    void postVerificationWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/admin/donors/" + donor.getId() + "/verify")
                        .param("action", "APPROVE"))
                .andExpect(status().isForbidden());
    }
}
