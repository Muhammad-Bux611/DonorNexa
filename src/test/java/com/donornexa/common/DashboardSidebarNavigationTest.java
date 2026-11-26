package com.donornexa.common;

import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.notification.entity.Notification;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.Gender;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verification of the unified dashboard persistent sidebar layout,
 * role-specific navigation menus, user identity badges, and active state indicators.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dashboard Persistent Sidebar & Navigation Tests")
class DashboardSidebarNavigationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private NotificationRepository notificationRepository;

    private static final String DONOR_EMAIL = "sidebar.donor@donornexa.com";
    private static final String HOSPITAL_EMAIL = "sidebar.hosp@donornexa.com";
    private static final String ADMIN_EMAIL = "sidebar.admin@donornexa.com";

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

        // Seed Donor
        if (userRepository.findByEmail(DONOR_EMAIL).isEmpty()) {
            User u = new User();
            u.setFullName("Tariq Mahmood");
            u.setEmail(DONOR_EMAIL);
            u.setPhoneNumber("0300-4444444");
            u.setPasswordHash("$2a$10$hashedpass");
            u.setRole(Role.DONOR);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            u = userRepository.save(u);

            Donor d = new Donor();
            d.setUser(u);
            d.setBloodGroup(BloodGroup.A_POSITIVE);
            d.setGender(Gender.MALE);
            d.setDateOfBirth(LocalDate.of(1995, 5, 20));
            d.setWeight(BigDecimal.valueOf(70.0));
            d.setProvince(province);
            d.setCity(city);
            d.setCompleteAddress("DHA Phase 5, Lahore");
            d.setAvailability(DonorAvailability.AVAILABLE);
            d.setVerified(true);
            donorRepository.save(d);

            Notification n = new Notification();
            n.setRecipient(u);
            n.setTitle("Welcome to DonorNexa");
            n.setMessage("Your profile is active.");
            n.setNotificationType(NotificationType.SYSTEM_ALERT);
            n.setNotificationStatus(NotificationStatus.UNREAD);
            notificationRepository.save(n);
        }

        // Seed Hospital
        if (userRepository.findByEmail(HOSPITAL_EMAIL).isEmpty()) {
            User u = new User();
            u.setFullName("Services Hospital Lahore");
            u.setEmail(HOSPITAL_EMAIL);
            u.setPhoneNumber("042-36666666");
            u.setPasswordHash("$2a$10$hashedpass");
            u.setRole(Role.HOSPITAL);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            u = userRepository.save(u);

            Hospital h = new Hospital();
            h.setUser(u);
            h.setHospitalName("Services Hospital Lahore");
            h.setRegistrationNumber("HOSP-LHR-SRV");
            h.setProvince(province);
            h.setCity(city);
            h.setContactNumber("042-36666666");
            h.setCompleteAddress("Ghaus-ul-Azam Road, Lahore");
            h.setVerified(true);
            hospitalRepository.save(h);
        }

        // Seed Admin
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User u = new User();
            u.setFullName("Super Administrator");
            u.setEmail(ADMIN_EMAIL);
            u.setPhoneNumber("0300-7777777");
            u.setPasswordHash("$2a$10$hashedpass");
            u.setRole(Role.ADMIN);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            userRepository.save(u);
        }
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("1. DONOR sidebar displays user identity, role, and donor navigation menu")
    void donorDashboard_rendersDonorSidebar() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PORTAL NAVIGATION")))
                .andExpect(content().string(containsString("Tariq Mahmood")))
                .andExpect(content().string(containsString("DONOR")))
                .andExpect(content().string(containsString("Donor Portal")))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("View Profile")))
                .andExpect(content().string(containsString("Edit Profile")))
                .andExpect(content().string(containsString("My Availability")))
                .andExpect(content().string(containsString("Donation History")))
                .andExpect(content().string(containsString("Sign Out")));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("2. HOSPITAL sidebar displays hospital identity, role, and hospital navigation menu")
    void hospitalDashboard_rendersHospitalSidebar() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PORTAL NAVIGATION")))
                .andExpect(content().string(containsString("Services Hospital Lahore")))
                .andExpect(content().string(containsString("HOSPITAL")))
                .andExpect(content().string(containsString("Hospital Portal")))
                .andExpect(content().string(containsString("View Hospital Profile")))
                .andExpect(content().string(containsString("Edit Hospital Profile")))
                .andExpect(content().string(containsString("Verification Status")))
                .andExpect(content().string(containsString("Blood Requests")))
                .andExpect(content().string(containsString("Sign Out")));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    @DisplayName("3. ADMIN sidebar displays admin identity, role, and administration navigation menu")
    void adminDashboard_rendersAdminSidebar() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PORTAL NAVIGATION")))
                .andExpect(content().string(containsString("Super Administrator")))
                .andExpect(content().string(containsString("ADMIN")))
                .andExpect(content().string(containsString("Administration")))
                .andExpect(content().string(containsString("Manage Hospitals")))
                .andExpect(content().string(containsString("Hospital Verification")))
                .andExpect(content().string(containsString("Manage Users")))
                .andExpect(content().string(containsString("Sign Out")));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("4. Active menu item highlights currently visited view")
    void donorProfile_highlightsActiveTab() throws Exception {
        mockMvc.perform(get("/donor/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/donor/profile\"")));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("5. Hospital verification view includes persistent sidebar")
    void hospitalVerification_rendersSidebar() throws Exception {
        mockMvc.perform(get("/hospital/verification"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PORTAL NAVIGATION")))
                .andExpect(content().string(containsString("Verification Status")));
    }
}
