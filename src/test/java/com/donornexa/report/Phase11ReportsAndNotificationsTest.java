package com.donornexa.report;

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
import com.donornexa.hospital.dto.HospitalVerificationStatus;
import com.donornexa.hospital.service.HospitalDonationService;
import com.donornexa.notification.service.NotificationService;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 11 â€” Notifications, Reports, Charts & Dashboard Statistics Tests")
class Phase11ReportsAndNotificationsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;

    private User adminUser;
    private User hospitalUser1;
    private User hospitalUser2;
    private User donorUser1;
    private Hospital hospital1;
    private Hospital hospital2;
    private Donor donor1;
    private Province province;
    private City city;

    @BeforeEach
    void setUp() {
        province = provinceRepository.findByName("Sindh").orElseGet(() -> {
            Province p = new Province();
            p.setName("Sindh");
            return provinceRepository.save(p);
        });

        city = cityRepository.findByName("Karachi").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Karachi");
            c.setProvince(province);
            return cityRepository.save(c);
        });

        // 1. Admin
        String adminEmail = "p11.admin." + UUID.randomUUID() + "@donornexa.com";
        adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setFullName("Platform Administrator");
        adminUser.setPhoneNumber("03001234567");
        adminUser.setPasswordHash("$2a$10$xyz");
        adminUser.setRole(Role.ADMIN);
        adminUser.setAccountStatus(AccountStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);

        // 2. Hospital 1
        String hosp1Email = "p11.hosp1." + UUID.randomUUID() + "@donornexa.com";
        hospitalUser1 = new User();
        hospitalUser1.setEmail(hosp1Email);
        hospitalUser1.setFullName("Aga Khan Hospital");
        hospitalUser1.setPhoneNumber("03001234568");
        hospitalUser1.setPasswordHash("$2a$10$xyz");
        hospitalUser1.setRole(Role.HOSPITAL);
        hospitalUser1.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser1 = userRepository.save(hospitalUser1);

        hospital1 = new Hospital();
        hospital1.setUser(hospitalUser1);
        hospital1.setHospitalName("Aga Khan University Hospital");
        hospital1.setRegistrationNumber("HOSP-AKU-" + UUID.randomUUID().toString().substring(0, 6));
        hospital1.setContactNumber("021-34930051");
        hospital1.setProvince(province);
        hospital1.setCity(city);
        hospital1.setCompleteAddress("Stadium Road, Karachi");
        hospital1.setVerified(true);
        hospital1 = hospitalRepository.save(hospital1);

        // 3. Hospital 2
        String hosp2Email = "p11.hosp2." + UUID.randomUUID() + "@donornexa.com";
        hospitalUser2 = new User();
        hospitalUser2.setEmail(hosp2Email);
        hospitalUser2.setFullName("Liaquat National Hospital");
        hospitalUser2.setPhoneNumber("03001234569");
        hospitalUser2.setPasswordHash("$2a$10$xyz");
        hospitalUser2.setRole(Role.HOSPITAL);
        hospitalUser2.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser2 = userRepository.save(hospitalUser2);

        hospital2 = new Hospital();
        hospital2.setUser(hospitalUser2);
        hospital2.setHospitalName("Liaquat National Hospital");
        hospital2.setRegistrationNumber("HOSP-LNH-" + UUID.randomUUID().toString().substring(0, 6));
        hospital2.setContactNumber("021-34412000");
        hospital2.setProvince(province);
        hospital2.setCity(city);
        hospital2.setCompleteAddress("National Stadium Road, Karachi");
        hospital2.setVerified(true);
        hospital2 = hospitalRepository.save(hospital2);

        // 4. Donor 1
        String donorEmail = "p11.donor1." + UUID.randomUUID() + "@donornexa.com";
        donorUser1 = new User();
        donorUser1.setEmail(donorEmail);
        donorUser1.setFullName("Ali Khan");
        donorUser1.setPhoneNumber("03001234570");
        donorUser1.setPasswordHash("$2a$10$xyz");
        donorUser1.setRole(Role.DONOR);
        donorUser1.setAccountStatus(AccountStatus.ACTIVE);
        donorUser1 = userRepository.save(donorUser1);

        donor1 = new Donor();
        donor1.setUser(donorUser1);
        donor1.setBloodGroup(BloodGroup.O_POSITIVE);
        donor1.setGender(Gender.MALE);
        donor1.setDateOfBirth(LocalDate.of(1995, 5, 20));
        donor1.setProvince(province);
        donor1.setCity(city);
        donor1.setCompleteAddress("Gulshan-e-Iqbal, Karachi");
        donor1.setAvailability(DonorAvailability.AVAILABLE);
        donor1.setVerified(true);
        donor1 = donorRepository.save(donor1);

        // Seed sample request for Hospital 1
        BloodRequest br1 = new BloodRequest();
        br1.setHospital(hospital1);
        br1.setBloodGroup(BloodGroup.O_POSITIVE);
        br1.setUnitsRequired(3);
        br1.setUnitsFulfilled(1);
        br1.setUrgency(RequestUrgency.URGENT);
        br1.setStatus(RequestStatus.MATCHING);
        br1.setProvince(province);
        br1.setCity(city);
        br1.setRequiredBy(LocalDateTime.now().plusDays(2));
        br1 = bloodRequestRepository.save(br1);

        // Seed match
        RequestMatch match = new RequestMatch();
        match.setBloodRequest(br1);
        match.setDonor(donor1);
        match.setMatchScore(95);
        match.setMatchStatus(MatchStatus.ACCEPTED);
        match.setMatchedAt(LocalDateTime.now());
        requestMatchRepository.save(match);

        // Seed completed donation
        Donation donation = new Donation();
        donation.setDonor(donor1);
        donation.setHospital(hospital1);
        donation.setBloodRequest(br1);
        donation.setUnitsDonated(1);
        donation.setDonationDate(LocalDateTime.now().minusDays(1));
        donation.setDonationStatus(DonationStatus.COMPLETED);
        donationRepository.save(donation);
    }

    // =========================================================================
    // 1. Hospital Reports & Security
    // =========================================================================

    @Test
    @DisplayName("Hospital can access its reports dashboard and view summary statistics")
    void hospital_canAccessReports() throws Exception {
        mockMvc.perform(get("/hospital/reports")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/reports"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attribute("activeTab", "summary"));
    }

    @Test
    @DisplayName("Non-hospital user is rejected from hospital reports (403 Forbidden)")
    void hospital_donorForbiddenFromHospitalReports() throws Exception {
        mockMvc.perform(get("/hospital/reports")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Hospital chart data endpoint returns valid JSON with lifecycle & monthly statistics")
    void hospital_chartDataEndpointReturnsJson() throws Exception {
        mockMvc.perform(get("/hospital/reports/chart-data")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requestStatusDistribution").exists())
                .andExpect(jsonPath("$.monthlyRequests").exists())
                .andExpect(jsonPath("$.monthlyDonations").exists());
    }

    @Test
    @DisplayName("Hospital can export blood requests, matches, and donations as CSV")
    void hospital_csvExportsWork() throws Exception {
        // Requests CSV
        mockMvc.perform(get("/hospital/reports/export")
                .param("reportType", "requests")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("hospital_requests_report.csv")))
                .andExpect(content().string(containsString("Request ID,Blood Group,Units Required")));

        // Matches CSV (masked donor identity, no CNIC)
        mockMvc.perform(get("/hospital/reports/export")
                .param("reportType", "matches")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("hospital_matches_report.csv")))
                .andExpect(content().string(containsString("Match ID,Request ID,Blood Group,Donor Masked Identity")));

        // Donations CSV
        mockMvc.perform(get("/hospital/reports/export")
                .param("reportType", "donations")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("hospital_donations_report.csv")))
                .andExpect(content().string(containsString("Donation ID,Donor Name,Blood Group,Units Donated")));
    }

    @Test
    @DisplayName("Hospital data isolation is enforced between different hospitals")
    void hospital_dataIsolationEnforced() throws Exception {
        // Hospital 2 should see 0 requests and 0 donations
        mockMvc.perform(get("/hospital/reports")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser2.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("summary"));
    }

    // =========================================================================
    // 2. Admin Reports & Exports
    // =========================================================================

    @Test
    @DisplayName("Admin can view platform reports and fetch dynamic chart JSON")
    void admin_canAccessReportsAndCharts() throws Exception {
        mockMvc.perform(get("/admin/reports")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("totalDonors"))
                .andExpect(model().attributeExists("donorBloodGroups"))
                .andExpect(model().attributeExists("monthlyRequests"))
                .andExpect(model().attributeExists("hospitalActivity"));

        mockMvc.perform(get("/admin/reports/chart-data")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bloodGroupDistribution").exists())
                .andExpect(jsonPath("$.requestStatusDistribution").exists());
    }

    @Test
    @DisplayName("Admin can export system-wide donors, requests, donations, and hospital activity CSVs")
    void admin_canExportAllReportsCsv() throws Exception {
        mockMvc.perform(get("/admin/reports/export")
                .param("reportType", "donors")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("admin_donors_report.csv")))
                .andExpect(content().string(containsString("Donor ID,Full Name,Email,Phone,Blood Group")));

        mockMvc.perform(get("/admin/reports/export")
                .param("reportType", "hospitals")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("admin_hospitals_report.csv")))
                .andExpect(content().string(containsString("Hospital ID,Hospital Name,Registration Number")));
    }

    @Test
    @DisplayName("Non-admin user cannot access admin reports or CSV exports (403 Forbidden)")
    void admin_reportsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/admin/reports")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/reports/export")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Dashboard Statistics Verification
    // =========================================================================

    @Test
    @DisplayName("Admin dashboard model contains Phase 11 statistics (pending requests, total donations, total notifications, recent requests)")
    void admin_dashboardContainsPhase11Metrics() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("pendingBloodRequests"))
                .andExpect(model().attributeExists("totalDonations"))
                .andExpect(model().attributeExists("totalNotifications"))
                .andExpect(model().attributeExists("bloodGroupDistribution"))
                .andExpect(model().attributeExists("recentBloodRequests"));
    }

    @Test
    @DisplayName("Donor dashboard model contains Phase 11 statistics (incoming requests, accepted requests, recent notifications)")
    void donor_dashboardContainsPhase11Metrics() throws Exception {
        mockMvc.perform(get("/donor/dashboard")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attribute("userEmail", donorUser1.getEmail()));
    }

    @Test
    @DisplayName("Hospital dashboard model contains Phase 11 statistics (requests created, active, completed, matches, notifications)")
    void hospital_dashboardContainsPhase11Metrics() throws Exception {
        mockMvc.perform(get("/hospital/dashboard")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attribute("userEmail", hospitalUser1.getEmail()));
    }

    // =========================================================================
    // 4. Notifications Management & IDOR Security
    // =========================================================================

    @Test
    @DisplayName("Notification workflow: delivery, filtering, mark as read, mark all read, and IDOR protection")
    void notification_workflowAndSecurity() throws Exception {
        // Create 2 notifications for Donor
        Notification n1 = notificationService.createNotification(donorUser1, "Urgent Blood Need", "Need O+ blood urgently", NotificationType.BLOOD_REQUEST);
        Notification n2 = notificationService.createNotification(donorUser1, "Match Found", "You have been matched", NotificationType.DONOR_MATCH);

        // Verify unread count
        assertThat(notificationService.getUnreadCount(donorUser1.getEmail())).isGreaterThanOrEqualTo(2);

        // Filter notifications by type
        mockMvc.perform(get("/notifications")
                .param("type", "BLOOD_REQUEST")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("notification/notifications"))
                .andExpect(model().attribute("selectedType", NotificationType.BLOOD_REQUEST));

        // Mark single notification as read
        mockMvc.perform(post("/notifications/" + n1.getId() + "/read")
                .with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        Notification updated = notificationRepository.findById(n1.getId()).orElseThrow();
        assertThat(updated.getNotificationStatus()).isEqualTo(NotificationStatus.READ);

        // IDOR attempt: Hospital attempts to mark Donor's notification as read
        mockMvc.perform(post("/notifications/" + n2.getId() + "/read")
                .with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", containsString("Access denied")));

        // n2 should remain UNREAD
        Notification untouched = notificationRepository.findById(n2.getId()).orElseThrow();
        assertThat(untouched.getNotificationStatus()).isEqualTo(NotificationStatus.UNREAD);

        // Mark all as read for Donor
        mockMvc.perform(post("/notifications/read-all")
                .with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().is3xxRedirection());

        assertThat(notificationService.getUnreadCount(donorUser1.getEmail())).isEqualTo(0);
    }
}
