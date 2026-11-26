package com.donornexa.admin;

import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.*;
import com.donornexa.donor.entity.*;
import com.donornexa.hospital.entity.*;
import com.donornexa.bloodrequest.entity.*;
import com.donornexa.matching.entity.*;
import com.donornexa.donation.entity.*;
import com.donornexa.inventory.entity.*;
import com.donornexa.notification.entity.*;
import com.donornexa.location.entity.*;
import com.donornexa.messaging.entity.*;
import com.donornexa.audit.entity.*;
import com.donornexa.admin.entity.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 9 â€” Comprehensive Administration Module Web Security and Route Tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 9 â€” Admin Web Security & Operations Tests")
class AdminFullModuleWebSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private BloodInventoryRepository bloodInventoryRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    private static final String ADMIN_EMAIL = "admin.phase9@donornexa.com";
    private static final String TARGET_USER_EMAIL = "target.user.phase9@donornexa.com";

    private User adminUser;
    private User targetUser;
    private BloodRequest testBloodRequest;
    private Donation testDonation;

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

        adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(ADMIN_EMAIL);
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("Phase 9 Lead Administrator");
            u.setPhoneNumber("03009999991");
            u.setRole(Role.ADMIN);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        targetUser = userRepository.findByEmail(TARGET_USER_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(TARGET_USER_EMAIL);
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("Target Test User");
            u.setPhoneNumber("03009999992");
            u.setRole(Role.DONOR);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        // Ensure target user status is ACTIVE before each test
        targetUser.setAccountStatus(AccountStatus.ACTIVE);
        targetUser.setEnabled(true);
        userRepository.save(targetUser);

        // Prepare Hospital
        User hospUser = userRepository.findByEmail("hospital.phase9@donornexa.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("hospital.phase9@donornexa.com");
            u.setPasswordHash("$2a$10$e8Z4z61gCslV1bI3wO5B9.fKj2X1f7B9xO7iC9V8E3eXyR0B6xVae");
            u.setFullName("General Hospital Lahore");
            u.setPhoneNumber("03009999993");
            u.setRole(Role.HOSPITAL);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        Hospital hospital = hospitalRepository.findByUserId(hospUser.getId()).orElseGet(() -> {
            Hospital h = new Hospital();
            h.setUser(hospUser);
            h.setHospitalName("General Hospital Lahore");
            h.setRegistrationNumber("GEN-HOSP-099");
            h.setProvince(province);
            h.setCity(city);
            h.setVerified(true);
            return hospitalRepository.save(h);
        });

        // Prepare Donor
        Donor donor = donorRepository.findByUserId(targetUser.getId()).orElseGet(() -> {
            Donor d = new Donor();
            d.setUser(targetUser);
            d.setBloodGroup(BloodGroup.A_POSITIVE);
            d.setGender(Gender.MALE);
            d.setDateOfBirth(LocalDate.of(1995, 1, 1));
            d.setProvince(province);
            d.setCity(city);
            d.setAvailability(DonorAvailability.AVAILABLE);
            d.setVerified(true);
            return donorRepository.save(d);
        });

        // Prepare Blood Request
        if (testBloodRequest == null) {
            BloodRequest br = new BloodRequest();
            br.setHospital(hospital);
            br.setBloodGroup(BloodGroup.A_POSITIVE);
            br.setUnitsRequired(3);
            br.setUnitsFulfilled(0);
            br.setUrgency(RequestUrgency.URGENT);
            br.setStatus(RequestStatus.PENDING);
            br.setProvince(province);
            br.setCity(city);
            br.setHospitalNotes("Emergency surgical request");
            testBloodRequest = bloodRequestRepository.save(br);
        }

        // Prepare Donation
        if (testDonation == null) {
            Donation don = new Donation();
            don.setDonor(donor);
            don.setHospital(hospital);
            don.setBloodRequest(testBloodRequest);
            don.setUnitsDonated(1);
            don.setDonationDate(LocalDateTime.now());
            don.setDonationStatus(DonationStatus.COMPLETED);
            testDonation = donationRepository.save(don);
        }

        // Prepare Inventory
        if (!bloodInventoryRepository.existsByHospitalIdAndBloodGroup(hospital.getId(), BloodGroup.A_POSITIVE)) {
            BloodInventory inv = new BloodInventory();
            inv.setHospital(hospital);
            inv.setBloodGroup(BloodGroup.A_POSITIVE);
            inv.setAvailableUnits(15);
            inv.setReservedUnits(2);
            inv.setLastUpdated(LocalDateTime.now());
            bloodInventoryRepository.save(inv);
        }
    }

    @Test
    @DisplayName("1. Anonymous user accessing /admin/dashboard is redirected to login")
    void anonymous_cannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "donor@test.com", roles = {"DONOR"})
    @DisplayName("2. Donor role accessing /admin/dashboard is forbidden (403)")
    void donorRole_cannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hospital@test.com", roles = {"HOSPITAL"})
    @DisplayName("3. Hospital role accessing /admin/dashboard is forbidden (403)")
    void hospitalRole_cannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("4. Admin accesses /admin/dashboard and sees comprehensive metrics")
    void admin_canAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("Platform Administration")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("5. Admin accesses /admin/users directory")
    void admin_canAccessUserDirectory() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(containsString("User Directory")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("6. Admin inspects individual user at /admin/users/{id}")
    void admin_canInspectUserDetails() throws Exception {
        mockMvc.perform(get("/admin/users/" + targetUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-details"))
                .andExpect(content().string(containsString(TARGET_USER_EMAIL)));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("7. Admin updates user account status to SUSPENDED")
    void admin_canUpdateUserStatus() throws Exception {
        mockMvc.perform(post("/admin/users/" + targetUser.getId() + "/status")
                        .with(csrf())
                        .param("accountStatus", "SUSPENDED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + targetUser.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertEquals(AccountStatus.SUSPENDED, updated.getAccountStatus());
        assertFalse(updated.getEnabled());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("8. Admin attempting to suspend their own account is rejected (Self-lockout guard)")
    void admin_cannotSuspendSelf() throws Exception {
        mockMvc.perform(post("/admin/users/" + adminUser.getId() + "/status")
                        .with(csrf())
                        .param("accountStatus", "SUSPENDED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/" + adminUser.getId()))
                .andExpect(flash().attributeExists("errorMessage"));

        User unchanged = userRepository.findById(adminUser.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, unchanged.getAccountStatus());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("9. Admin accesses /admin/verification hub")
    void admin_canAccessVerificationHub() throws Exception {
        mockMvc.perform(get("/admin/verification"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/verification"))
                .andExpect(content().string(containsString("Verification Central Hub")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("10. Admin accesses /admin/blood-requests list")
    void admin_canAccessBloodRequests() throws Exception {
        mockMvc.perform(get("/admin/blood-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blood-requests"))
                .andExpect(content().string(containsString("Emergency Blood Requests")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("11. Admin inspects blood request details and updates status")
    void admin_canInspectAndUpdateBloodRequest() throws Exception {
        mockMvc.perform(get("/admin/blood-requests/" + testBloodRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/blood-request-details"));

        mockMvc.perform(post("/admin/blood-requests/" + testBloodRequest.getId() + "/status")
                        .with(csrf())
                        .param("status", "MATCHING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blood-requests/" + testBloodRequest.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        BloodRequest updated = bloodRequestRepository.findById(testBloodRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.MATCHING, updated.getStatus());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("12. Admin accesses /admin/donations list and details")
    void admin_canAccessDonations() throws Exception {
        mockMvc.perform(get("/admin/donations"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/donations"))
                .andExpect(content().string(containsString("Clinical Donation Records")));

        mockMvc.perform(get("/admin/donations/" + testDonation.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/donation-details"));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("13. Admin accesses /admin/inventory centralized view")
    void admin_canAccessInventory() throws Exception {
        mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andExpect(content().string(containsString("Centralized Blood Inventory")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("14. Admin accesses /admin/audit-logs viewer")
    void admin_canAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/audit-logs"))
                .andExpect(content().string(containsString("Security &amp; Compliance Audit Logs")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("15. Admin accesses /admin/reports analytics dashboard")
    void admin_canAccessReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(content().string(containsString("Platform Analytics")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("16. Admin accesses /admin/settings configuration view")
    void admin_canAccessSettings() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings"))
                .andExpect(content().string(containsString("Platform Architecture &amp; System Settings")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("17. Admin accesses /admin/profile view")
    void admin_canAccessProfile() throws Exception {
        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profile"))
                .andExpect(content().string(containsString("Administrator Profile")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("18. POST operation without CSRF is rejected (403 Forbidden)")
    void postWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/admin/users/" + targetUser.getId() + "/status")
                        .param("accountStatus", "SUSPENDED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("19. Anonymous visitor can access /admin/login page")
    void anonymous_canAccessAdminLogin() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"))
                .andExpect(content().string(containsString("Administrator Portal")))
                .andExpect(content().string(containsString("Sign In to Admin Portal")));
    }

    @Test
    @DisplayName("20. Public home page displays Administrator Portal access")
    void homePage_displaysAdminPortalAccess() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/home"))
                .andExpect(content().string(containsString("Administrator Portal")))
                .andExpect(content().string(containsString("/admin/login")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("21. Admin accesses /admin/verifications/donors route")
    void admin_canAccessVerificationDonors() throws Exception {
        mockMvc.perform(get("/admin/verifications/donors"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/donors?verificationStatus=PENDING"));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("22. Admin accesses /admin/verifications/hospitals route")
    void admin_canAccessVerificationHospitals() throws Exception {
        mockMvc.perform(get("/admin/verifications/hospitals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals?verificationStatus=PENDING"));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("23. Authorized Admin can provision a new administrator")
    void admin_canCreateNewAdministrator() throws Exception {
        mockMvc.perform(post("/admin/users/create-admin")
                        .with(csrf())
                        .param("fullName", "Assistant Administrator")
                        .param("email", "assistant.admin@donornexa.com")
                        .param("phoneNumber", "03008888888")
                        .param("password", "Admin@Secure2026!")
                        .param("confirmPassword", "Admin@Secure2026!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        User created = userRepository.findByEmail("assistant.admin@donornexa.com").orElse(null);
        assertNotNull(created);
        assertEquals(Role.ADMIN, created.getRole());
        assertEquals(AccountStatus.ACTIVE, created.getAccountStatus());
    }

    @Test
    @WithMockUser(username = "donor.phase9@donornexa.com", roles = {"DONOR"})
    @DisplayName("24. Non-admin user cannot invoke /admin/users/create-admin (403 Forbidden)")
    void nonAdmin_cannotCreateAdministrator() throws Exception {
        mockMvc.perform(post("/admin/users/create-admin")
                        .with(csrf())
                        .param("fullName", "Attacker")
                        .param("email", "hacker@donornexa.com")
                        .param("phoneNumber", "03009999999")
                        .param("password", "HackedPass123!")
                        .param("confirmPassword", "HackedPass123!"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("25. Admin accesses /admin/notifications communications oversight endpoint")
    void admin_canAccessNotificationsAndCommunicationMonitoring() throws Exception {
        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notifications"))
                .andExpect(model().attributeExists("notificationPage", "totalNotifications", "totalConversations", "totalMessages"));
    }

    @Test
    @WithMockUser(username = "donor.phase9@donornexa.com", roles = {"DONOR"})
    @DisplayName("26. Non-admin user cannot access /admin/notifications (403 Forbidden)")
    void nonAdmin_cannotAccessNotificationsMonitoring() throws Exception {
        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isForbidden());
    }
}
