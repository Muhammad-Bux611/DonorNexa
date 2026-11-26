package com.donornexa.matching;

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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 7 â€” Matching Web Security & Controller Tests")
class MatchingWebSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;

    private Hospital hospital;
    private User hospitalUser;
    private Donor donor;
    private User donorUser;
    private BloodRequest bloodRequest;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        Province province = provinceRepository.save(createProvince("Prov-" + uid));
        City city = cityRepository.save(createCity("City-" + uid, province));

        // Create Hospital
        hospitalUser = userRepository.save(createUser("hosp.sec." + uid + "@donornexa.com", Role.HOSPITAL));
        hospital = new Hospital();
        hospital.setUser(hospitalUser);
        hospital.setHospitalName("Sec Hospital " + uid);
        hospital.setRegistrationNumber("REG-SEC-" + uid);
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setVerified(true);
        hospital = hospitalRepository.save(hospital);

        // Create Donor
        donorUser = userRepository.save(createUser("donor.sec." + uid + "@donornexa.com", Role.DONOR));
        donor = new Donor();
        donor.setUser(donorUser);
        donor.setBloodGroup(BloodGroup.O_POSITIVE);
        donor.setDateOfBirth(LocalDate.now().minusYears(24));
        donor.setGender(Gender.MALE);
        donor.setWeight(BigDecimal.valueOf(70.0));
        donor.setProvince(province);
        donor.setCity(city);
        donor.setAvailability(DonorAvailability.AVAILABLE);
        donor.setVerified(true);
        donor = donorRepository.save(donor);

        // Create Blood Request
        bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodGroup(BloodGroup.O_POSITIVE);
        bloodRequest.setUnitsRequired(3);
        bloodRequest.setUnitsFulfilled(0);
        bloodRequest.setUrgency(RequestUrgency.URGENT);
        bloodRequest.setStatus(RequestStatus.PENDING);
        bloodRequest.setProvince(province);
        bloodRequest.setCity(city);
        bloodRequest.setRequiredBy(LocalDateTime.now().plusDays(2));
        bloodRequest = bloodRequestRepository.save(bloodRequest);
    }

    private Province createProvince(String name) {
        Province p = new Province();
        p.setName(name);
        return p;
    }

    private City createCity(String name, Province p) {
        City c = new City();
        c.setName(name);
        c.setProvince(p);
        return c;
    }

    private User createUser(String email, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setFullName("Security Test " + email);
        u.setPhoneNumber("0322" + (int)(Math.random() * 8999999 + 1000000));
        u.setPasswordHash("$2a$10$dummyHashGeneratedForTestPurposesOnly12345");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setEnabled(true);
        return u;
    }

    // =========================================================================
    // 1. Hospital Blood Request & Matching Access Tests
    // =========================================================================

    @Test
    @DisplayName("Hospital user can access blood requests dashboard")
    void hospital_accessBloodRequestsDashboard_success() throws Exception {
        mockMvc.perform(get("/hospital/blood-requests")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/blood-requests"))
                .andExpect(model().attributeExists("requestPage"));
    }

    @Test
    @DisplayName("Hospital user can access create blood request page")
    void hospital_accessCreateRequestPage_success() throws Exception {
        mockMvc.perform(get("/hospital/blood-requests/new")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/create-blood-request"))
                .andExpect(model().attributeExists("requestForm"));
    }

    @Test
    @DisplayName("Hospital user can access matching results for own request")
    void hospital_accessMatchesForOwnRequest_success() throws Exception {
        mockMvc.perform(get("/hospital/blood-requests/{id}/matches", bloodRequest.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospitalUser.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/matches"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    @DisplayName("Donor cannot access hospital blood request pages (403 Forbidden)")
    @WithMockUser(username = "donor@example.com", roles = "DONOR")
    void donor_accessHospitalBloodRequests_forbidden() throws Exception {
        mockMvc.perform(get("/hospital/blood-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated access to hospital blood requests redirects to login")
    void unauthenticated_accessHospitalBloodRequests_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/hospital/blood-requests"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // 2. Donor Matching Portal Access Tests
    // =========================================================================

    @Test
    @DisplayName("Donor can access My Matches portal")
    void donor_accessMyMatches_success() throws Exception {
        mockMvc.perform(get("/donor/matches")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/matches"))
                .andExpect(model().attributeExists("matchPage"));
    }

    @Test
    @DisplayName("Hospital user cannot access donor matches portal (403 Forbidden)")
    @WithMockUser(username = "hospital@example.com", roles = "HOSPITAL")
    void hospital_accessDonorMatches_forbidden() throws Exception {
        mockMvc.perform(get("/donor/matches"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Administrator Matching Oversight Access Tests
    // =========================================================================

    @Test
    @DisplayName("Admin can access matching oversight portal")
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    void admin_accessMatchingOversight_success() throws Exception {
        mockMvc.perform(get("/admin/matches"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/matches"))
                .andExpect(model().attributeExists("matchPage"));
    }

    @Test
    @DisplayName("Donor user cannot access admin matching oversight (403 Forbidden)")
    @WithMockUser(username = "donor@example.com", roles = "DONOR")
    void donor_accessAdminMatches_forbidden() throws Exception {
        mockMvc.perform(get("/admin/matches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Hospital user cannot access admin matching oversight (403 Forbidden)")
    @WithMockUser(username = "hospital@example.com", roles = "HOSPITAL")
    void hospital_accessAdminMatches_forbidden() throws Exception {
        mockMvc.perform(get("/admin/matches"))
                .andExpect(status().isForbidden());
    }
}
