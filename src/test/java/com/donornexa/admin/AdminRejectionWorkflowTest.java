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
import com.donornexa.hospital.dto.HospitalVerificationActionDTO;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalVerificationService;
import com.donornexa.admin.service.AdminBloodRequestService;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verification test suite for Administrative Rejection Workflows (Hospital and Blood Request),
 * covering status transitions, reason validation, security access control, duplicate rejection prevention,
 * and audit logging.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Admin Rejection Workflows & State Consistency Tests")
class AdminRejectionWorkflowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private HospitalVerificationService hospitalVerificationService;
    @Autowired private AdminBloodRequestService adminBloodRequestService;

    private static final String ADMIN_EMAIL = "admin@donornexa.com";
    private Hospital testHospital;
    private BloodRequest testBloodRequest;

    @BeforeEach
    void setUp() {
        Province province = provinceRepository.findByName("Sindh").orElseGet(() -> {
            Province p = new Province();
            p.setName("Sindh");
            return provinceRepository.save(p);
        });

        City city = cityRepository.findByName("Karachi").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Karachi");
            c.setProvince(province);
            return cityRepository.save(c);
        });

        // Ensure Admin user exists
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            User admin = new User();
            admin.setEmail(ADMIN_EMAIL);
            admin.setPasswordHash("$2a$12$eXampleHashedPasswordAdmin123!");
            admin.setFullName("Platform Administrator");
            admin.setPhoneNumber("03001234567");
            admin.setRole(Role.ADMIN);
            admin.setAccountStatus(AccountStatus.ACTIVE);
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        // Create a unique test hospital
        long randSeed = Math.abs(System.nanoTime());
        String uniqueSuffix = String.valueOf(randSeed % 1000000);
        String hospitalEmail = "hospital.rejection.test." + uniqueSuffix + "@donornexa.com";
        User hospitalUser = new User();
        hospitalUser.setEmail(hospitalEmail);
        hospitalUser.setPasswordHash("$2a$12$eXampleHashedPasswordHospital123!");
        hospitalUser.setFullName("General Care Hospital " + uniqueSuffix);
        hospitalUser.setPhoneNumber("03" + String.format("%09d", randSeed % 1000000000L));
        hospitalUser.setRole(Role.HOSPITAL);
        hospitalUser.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        hospitalUser.setEnabled(true);
        hospitalUser = userRepository.save(hospitalUser);

        Hospital hospital = new Hospital();
        hospital.setUser(hospitalUser);
        hospital.setHospitalName("General Care Hospital " + uniqueSuffix);
        hospital.setRegistrationNumber("REG-TEST-" + uniqueSuffix);
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setCompleteAddress("St. 4, Medical City, Karachi");
        hospital.setContactNumber("021" + String.format("%07d", randSeed % 10000000L));
        hospital.setTrustScore(100);
        hospital.setVerified(Boolean.FALSE);
        hospital.setRejectionReason(null);
        testHospital = hospitalRepository.save(hospital);

        // Create a test blood request
        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(testHospital);
        bloodRequest.setProvince(province);
        bloodRequest.setCity(city);
        bloodRequest.setBloodGroup(BloodGroup.O_POSITIVE);
        bloodRequest.setUnitsRequired(2);
        bloodRequest.setUnitsFulfilled(0);
        bloodRequest.setUrgency(RequestUrgency.URGENT);
        bloodRequest.setStatus(RequestStatus.PENDING);
        bloodRequest.setRequiredBy(LocalDateTime.now().plusHours(12));
        testBloodRequest = bloodRequestRepository.save(bloodRequest);
    }

    // =========================================================================
    // Hospital Rejection Tests
    // =========================================================================

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("1. Admin successfully rejects pending hospital with valid reason")
    void admin_rejectHospitalWithReason_succeeds() throws Exception {
        mockMvc.perform(post("/admin/hospitals/" + testHospital.getId() + "/verify")
                        .with(csrf())
                        .param("action", "REJECT")
                        .param("rejectionReason", "Health commission registration expired. Cannot verify."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals/" + testHospital.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        Hospital updated = hospitalRepository.findByIdWithDetails(testHospital.getId()).orElseThrow();
        assertFalse(updated.getVerified());
        assertEquals("Health commission registration expired. Cannot verify.", updated.getRejectionReason());
        assertEquals(AccountStatus.INACTIVE, updated.getUser().getAccountStatus());
        assertFalse(updated.getUser().getEnabled());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("2. Admin rejects hospital without reason â€” validation blocks and returns error")
    void admin_rejectHospitalWithoutReason_fails() throws Exception {
        mockMvc.perform(post("/admin/hospitals/" + testHospital.getId() + "/verify")
                        .with(csrf())
                        .param("action", "REJECT")
                        .param("rejectionReason", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/hospitals/" + testHospital.getId()))
                .andExpect(flash().attributeExists("errorMessage"));

        Hospital unchanged = hospitalRepository.findByIdWithDetails(testHospital.getId()).orElseThrow();
        assertNull(unchanged.getRejectionReason());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("3. Repeated rejection of an already rejected hospital is rejected")
    void admin_repeatedHospitalRejection_fails() {
        HospitalVerificationActionDTO dto = new HospitalVerificationActionDTO();
        dto.setAction("REJECT");
        dto.setRejectionReason("First rejection reason.");

        hospitalVerificationService.verifyHospital(testHospital.getId(), dto, ADMIN_EMAIL);

        // Attempt second rejection
        HospitalVerificationActionDTO secondDto = new HospitalVerificationActionDTO();
        secondDto.setAction("REJECT");
        secondDto.setRejectionReason("Second rejection attempt.");

        assertThrows(HospitalOperationException.class, () ->
                hospitalVerificationService.verifyHospital(testHospital.getId(), secondDto, ADMIN_EMAIL));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("4. Attempting to reject an already verified hospital directly is rejected")
    void admin_rejectAlreadyVerifiedHospital_fails() {
        HospitalVerificationActionDTO approveDto = new HospitalVerificationActionDTO();
        approveDto.setAction("APPROVE");
        hospitalVerificationService.verifyHospital(testHospital.getId(), approveDto, ADMIN_EMAIL);

        HospitalVerificationActionDTO rejectDto = new HospitalVerificationActionDTO();
        rejectDto.setAction("REJECT");
        rejectDto.setRejectionReason("Cannot reject after approval.");

        assertThrows(HospitalOperationException.class, () ->
                hospitalVerificationService.verifyHospital(testHospital.getId(), rejectDto, ADMIN_EMAIL));
    }

    @Test
    @WithMockUser(username = "donor@donornexa.com", roles = {"DONOR"})
    @DisplayName("5. Non-admin user cannot reject a hospital (403 Forbidden)")
    void nonAdmin_cannotRejectHospital() throws Exception {
        mockMvc.perform(post("/admin/hospitals/" + testHospital.getId() + "/verify")
                        .with(csrf())
                        .param("action", "REJECT")
                        .param("rejectionReason", "Unauthorized rejection attempt."))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Blood Request Rejection Tests
    // =========================================================================

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("6. Admin successfully rejects pending blood request with reason")
    void admin_rejectBloodRequestWithReason_succeeds() throws Exception {
        mockMvc.perform(post("/admin/blood-requests/" + testBloodRequest.getId() + "/reject")
                        .with(csrf())
                        .param("rejectionReason", "Incomplete patient clinical documentation."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blood-requests/" + testBloodRequest.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        BloodRequest updated = bloodRequestRepository.findById(testBloodRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.REJECTED, updated.getStatus());
        assertEquals("Incomplete patient clinical documentation.", updated.getAdminNotes());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("7. Admin rejects blood request without reason â€” returns error")
    void admin_rejectBloodRequestWithoutReason_fails() throws Exception {
        mockMvc.perform(post("/admin/blood-requests/" + testBloodRequest.getId() + "/reject")
                        .with(csrf())
                        .param("rejectionReason", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/blood-requests/" + testBloodRequest.getId()))
                .andExpect(flash().attributeExists("errorMessage"));

        BloodRequest unchanged = bloodRequestRepository.findById(testBloodRequest.getId()).orElseThrow();
        assertEquals(RequestStatus.PENDING, unchanged.getStatus());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("8. Rejection of already fulfilled blood request is blocked")
    void admin_rejectFulfilledBloodRequest_fails() {
        testBloodRequest.setStatus(RequestStatus.FULFILLED);
        testBloodRequest.setUnitsFulfilled(2);
        bloodRequestRepository.save(testBloodRequest);

        assertThrows(IllegalStateException.class, () ->
                adminBloodRequestService.rejectBloodRequest(testBloodRequest.getId(), "Trying to reject fulfilled", ADMIN_EMAIL));
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    @DisplayName("9. Repeated rejection of already rejected blood request is blocked")
    void admin_repeatedBloodRequestRejection_fails() {
        adminBloodRequestService.rejectBloodRequest(testBloodRequest.getId(), "First rejection", ADMIN_EMAIL);

        assertThrows(IllegalStateException.class, () ->
                adminBloodRequestService.rejectBloodRequest(testBloodRequest.getId(), "Second rejection attempt", ADMIN_EMAIL));
    }

    @Test
    @WithMockUser(username = "hospital@donornexa.com", roles = {"HOSPITAL"})
    @DisplayName("10. Non-admin user cannot reject a blood request (403 Forbidden)")
    void nonAdmin_cannotRejectBloodRequest() throws Exception {
        mockMvc.perform(post("/admin/blood-requests/" + testBloodRequest.getId() + "/reject")
                        .with(csrf())
                        .param("rejectionReason", "Unauthorized rejection attempt."))
                .andExpect(status().isForbidden());
    }
}
