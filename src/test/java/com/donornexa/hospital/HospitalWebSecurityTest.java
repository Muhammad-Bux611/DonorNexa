package com.donornexa.hospital;

import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.hospital.repository.HospitalRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 â€” Web Security & Controller Tests (MockMvc).
 * Verifies role-based access restrictions, unauthenticated redirects,
 * hospital self-management, administrative verifications, and CSRF protection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Phase 5 â€” Hospital Web Security & Controller Tests")
class HospitalWebSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    private static final String HOSPITAL_EMAIL = "shifa@hospital.org";
    private Hospital hospital;
    private Province province;
    private City city;

    @BeforeEach
    void setUp() {
        province = provinceRepository.findByName("Punjab").orElseGet(() -> {
            Province p = new Province();
            p.setName("Punjab");
            return provinceRepository.save(p);
        });

        city = cityRepository.findByName("Lahore").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Lahore");
            c.setProvince(province);
            return cityRepository.save(c);
        });

        // Ensure clean hospital record
        hospital = hospitalRepository.findByUserEmailWithDetails(HOSPITAL_EMAIL).orElse(null);
        if (hospital == null) {
            User user = new User();
            user.setFullName("Shifa Hospital Lahore");
            user.setEmail(HOSPITAL_EMAIL);
            user.setPhoneNumber("042-35555555");
            user.setPasswordHash("$2a$10$hashedpass");
            user.setRole(Role.HOSPITAL);
            user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
            user.setEnabled(true);
            user = userRepository.save(user);

            hospital = new Hospital();
            hospital.setUser(user);
            hospital.setHospitalName("Shifa Hospital Lahore");
            hospital.setRegistrationNumber("HOSP-LHR-SHIFA");
            hospital.setProvince(province);
            hospital.setCity(city);
            hospital.setContactNumber("042-35555555");
            hospital.setCompleteAddress("Ferozepur Road, Lahore");
            hospital.setVerified(false);
            hospital = hospitalRepository.save(hospital);
        } else {
            hospital.setHospitalName("Shifa Hospital Lahore");
            hospital.setContactNumber("042-35555555");
            hospital.setCompleteAddress("Ferozepur Road, Lahore");
            hospital.setProvince(province);
            hospital.setCity(city);
            hospital.setVerified(false);
            hospital.setRejectionReason(null);
            hospital.getUser().setAccountStatus(AccountStatus.PENDING_VERIFICATION);
            hospital.getUser().setEnabled(true);
            userRepository.save(hospital.getUser());
            hospital = hospitalRepository.save(hospital);
        }
    }

    // =========================================================================
    // 1. Unauthenticated Access
    // =========================================================================

    @Test
    @DisplayName("1. Unauthenticated request to /hospital/dashboard redirects to /login")
    void unauthenticated_hospitalDashboard_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("2. Unauthenticated request to /admin/hospitals redirects to /login")
    void unauthenticated_adminHospitals_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/hospitals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // =========================================================================
    // 2. Role-Based Access Restrictions
    // =========================================================================

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = "DONOR")
    @DisplayName("3. DONOR role accessing /hospital/dashboard is forbidden (403)")
    void donorRole_cannotAccessHospitalDashboard() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = "DONOR")
    @DisplayName("4. DONOR role accessing /admin/hospitals is forbidden (403)")
    void donorRole_cannotAccessAdminHospitals() throws Exception {
        mockMvc.perform(get("/admin/hospitals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("5. HOSPITAL role accessing /admin/hospitals is forbidden (403)")
    void hospitalRole_cannotAccessAdminHospitals() throws Exception {
        mockMvc.perform(get("/admin/hospitals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("6. HOSPITAL role attempting to verify itself receives 403 Forbidden")
    void hospitalRole_cannotApproveItself() throws Exception {
        mockMvc.perform(post("/admin/hospitals/{id}/verify", hospital.getId())
                        .with(csrf())
                        .param("action", "APPROVE"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Hospital Self-Management
    // =========================================================================

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("7. HOSPITAL role can access its dashboard")
    void hospitalRole_canAccessDashboard() throws Exception {
        mockMvc.perform(get("/hospital/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/dashboard"))
                .andExpect(content().string(containsString("Shifa Hospital Lahore")));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("8. HOSPITAL role can access its profile page")
    void hospitalRole_canAccessProfile() throws Exception {
        mockMvc.perform(get("/hospital/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/profile"))
                .andExpect(content().string(containsString("HOSP-LHR-SHIFA")));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("9. HOSPITAL role can access profile edit form")
    void hospitalRole_canAccessEditProfileForm() throws Exception {
        mockMvc.perform(get("/hospital/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/edit-profile"))
                .andExpect(content().string(containsString("Edit Hospital Profile")));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("10. HOSPITAL role can submit valid profile update with CSRF")
    void hospitalRole_canSubmitProfileUpdate() throws Exception {
        mockMvc.perform(post("/hospital/profile/edit")
                        .with(csrf())
                        .param("hospitalName", "Shifa International Hospital Lahore")
                        .param("contactNumber", "042-35555555")
                        .param("provinceId", province.getId().toString())
                        .param("cityId", city.getId().toString())
                        .param("completeAddress", "Main Boulevard Gulberg, Lahore"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hospital/profile"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = HOSPITAL_EMAIL, roles = "HOSPITAL")
    @DisplayName("11. Profile update POST without CSRF token is forbidden (403)")
    void hospitalRole_profileUpdateWithoutCsrf_forbidden() throws Exception {
        mockMvc.perform(post("/hospital/profile/edit")
                        .param("hospitalName", "Shifa Hospital Lahore")
                        .param("contactNumber", "042-35555555")
                        .param("provinceId", province.getId().toString())
                        .param("cityId", city.getId().toString())
                        .param("completeAddress", "Ferozepur Road, Lahore"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. Administrator Hospital Management
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    @DisplayName("12. ADMIN role can access hospital listing directory")
    void adminRole_canAccessHospitalsList() throws Exception {
        mockMvc.perform(get("/admin/hospitals"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/hospitals"))
                .andExpect(content().string(containsString("Hospital Directory")));
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    @DisplayName("13. ADMIN role can inspect individual hospital details")
    void adminRole_canInspectHospitalDetails() throws Exception {
        mockMvc.perform(get("/admin/hospitals/{id}", hospital.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/hospital-details"))
                .andExpect(content().string(containsString("HOSP-LHR-SHIFA")));
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    @DisplayName("14. ADMIN role can approve hospital verification with CSRF")
    void adminRole_canApproveHospitalVerification() throws Exception {
        mockMvc.perform(post("/admin/hospitals/{id}/verify", hospital.getId())
                        .with(csrf())
                        .param("action", "APPROVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals/" + hospital.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    @DisplayName("15. ADMIN role can update hospital account status to SUSPENDED with CSRF")
    void adminRole_canSuspendHospitalAccount() throws Exception {
        mockMvc.perform(post("/admin/hospitals/{id}/status", hospital.getId())
                        .with(csrf())
                        .param("accountStatus", "SUSPENDED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals/" + hospital.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
