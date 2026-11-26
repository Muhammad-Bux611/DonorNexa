package com.donornexa.donor;

import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.Gender;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 4 â€” Web Security & Controller Tests (MockMvc).
 * Verifies role-based access restrictions, unauthenticated redirects,
 * authenticated donor views, form submission, and CSRF protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 4 â€” Donor Web Security & Controller Tests")
class DonorWebSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    private static final String DONOR_EMAIL = "tested.donor@donornexa.com";
    private Province punjab;
    private City lahore;

    @BeforeEach
    void ensureDonorExists() {
        punjab = provinceRepository.findByName("Punjab").orElseGet(() -> {
            Province p = new Province();
            p.setName("Punjab");
            return provinceRepository.save(p);
        });

        lahore = cityRepository.findByName("Lahore").stream()
                .filter(c -> c.getProvince().getId().equals(punjab.getId()))
                .findFirst()
                .orElseGet(() -> {
                    City c = new City();
                    c.setName("Lahore");
                    c.setProvince(punjab);
                    return cityRepository.save(c);
                });

        User user = userRepository.findByEmail(DONOR_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(DONOR_EMAIL);
            u.setPasswordHash("$2a$10$hashedpasswordplaceholder");
            u.setRole(Role.DONOR);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return u;
        });
        user.setFullName("Ahmad Raza");
        user.setPhoneNumber("0321-9999000");
        user = userRepository.save(user);

        User finalUser = user;
        Donor donor = donorRepository.findByUserId(user.getId()).orElseGet(() -> {
            Donor d = new Donor();
            d.setUser(finalUser);
            d.setBloodGroup(BloodGroup.O_POSITIVE);
            d.setGender(Gender.MALE);
            d.setDateOfBirth(LocalDate.of(1995, 6, 15));
            return d;
        });
        donor.setWeight(BigDecimal.valueOf(68.0));
        donor.setProvince(punjab);
        donor.setCity(lahore);
        donor.setCompleteAddress("Johar Town, Lahore");
        donor.setAvailability(DonorAvailability.AVAILABLE);
        donor.setVerified(true);
        donorRepository.save(donor);
    }

    // =========================================================================
    // 1. Unauthenticated redirects to login
    // =========================================================================

    @Test
    @DisplayName("1. Unauthenticated: /donor/profile redirects to login")
    void profile_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("2. Unauthenticated: /donor/profile/edit redirects to login")
    void editProfile_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/profile/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("3. Unauthenticated: /donor/availability redirects to login")
    void availability_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/availability"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("4. Unauthenticated: /donor/donations redirects to login")
    void donations_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/donor/donations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // 2. Role-based isolation (Hospital & Admin forbidden from /donor/**)
    // =========================================================================

    @Test
    @WithMockUser(username = "hospital@test.com", roles = "HOSPITAL")
    @DisplayName("5. Hospital role: forbidden from /donor/profile (403)")
    void hospitalRole_forbiddenFromDonorProfile() throws Exception {
        mockMvc.perform(get("/donor/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("6. Admin role: forbidden from /donor/profile (403)")
    void adminRole_forbiddenFromDonorProfile() throws Exception {
        mockMvc.perform(get("/donor/profile"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Authenticated Donor access
    // =========================================================================

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("7. Donor: can access dashboard with real data")
    void donorRole_canAccessDashboard() throws Exception {
        mockMvc.perform(get("/donor/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ahmad Raza")))
                .andExpect(content().string(containsString("Donor Portal Management")));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("8. Donor: can access profile view")
    void donorRole_canAccessProfile() throws Exception {
        mockMvc.perform(get("/donor/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ahmad Raza")))
                .andExpect(content().string(containsString("O +")))
                .andExpect(content().string(containsString("Johar Town, Lahore")));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("9. Donor: can access edit profile form")
    void donorRole_canAccessEditProfileForm() throws Exception {
        mockMvc.perform(get("/donor/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Donor Profile")))
                .andExpect(content().string(containsString("location-dropdown.js")));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("10. Donor: can submit valid profile update")
    void donorRole_canSubmitProfileUpdate() throws Exception {
        mockMvc.perform(post("/donor/profile/edit")
                        .param("fullName", "Ahmad Raza Updated")
                        .param("phoneNumber", "0321-9999000")
                        .param("weight", "71.5")
                        .param("provinceId", punjab.getId().toString())
                        .param("cityId", lahore.getId().toString())
                        .param("completeAddress", "Wapda Town, Lahore")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("11. Donor: can access availability management view")
    void donorRole_canAccessAvailability() throws Exception {
        mockMvc.perform(get("/donor/availability"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Donation Availability")))
                .andExpect(content().string(containsString("TEMPORARILY_UNAVAILABLE")));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("12. Donor: can submit availability update")
    void donorRole_canSubmitAvailabilityUpdate() throws Exception {
        mockMvc.perform(post("/donor/availability")
                        .param("availability", "TEMPORARILY_UNAVAILABLE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/availability"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = DONOR_EMAIL, roles = "DONOR")
    @DisplayName("13. Donor: can access donation history view")
    void donorRole_canAccessDonationHistory() throws Exception {
        mockMvc.perform(get("/donor/donations"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Donation History")));
    }
}
