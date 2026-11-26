package com.donornexa.hospital;

import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.hospital.dto.*;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalService;
import com.donornexa.hospital.service.HospitalVerificationService;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 5 â€” Integration Tests for {@link HospitalService} and {@link HospitalVerificationService}.
 * Verifies profile retrieval, permitted updates, immutable field protections,
 * location validation, verification approvals/rejections, and account lifecycle transitions.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Phase 5 â€” Hospital Service & Verification Tests")
class HospitalServiceTest {

    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private HospitalService hospitalService;
    @Autowired private HospitalVerificationService verificationService;
    @Autowired private EntityManager em;

    private Province punjab;
    private Province sindh;
    private City lahore;
    private City karachi;

    private User hospUser1;
    private User hospUser2;
    private User adminUser;
    private Hospital hospital1;
    private Hospital hospital2;

    @BeforeEach
    void setUp() {
        punjab = provinceRepository.findByName("Punjab").orElseGet(() -> {
            Province p = new Province();
            p.setName("Punjab");
            return provinceRepository.save(p);
        });

        sindh = provinceRepository.findByName("Sindh").orElseGet(() -> {
            Province p = new Province();
            p.setName("Sindh");
            return provinceRepository.save(p);
        });

        lahore = cityRepository.findByName("Lahore").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Lahore");
            c.setProvince(punjab);
            return cityRepository.save(c);
        });

        karachi = cityRepository.findByName("Karachi").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Karachi");
            c.setProvince(sindh);
            return cityRepository.save(c);
        });

        // Hospital 1 (Pending Verification)
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        hospUser1 = new User();
        hospUser1.setFullName("General Hospital");
        hospUser1.setEmail("general." + suffix1 + "@hospital.org");
        hospUser1.setPhoneNumber("042-3" + suffix1.substring(0, 6));
        hospUser1.setPasswordHash("$2a$10$hashedpass");
        hospUser1.setRole(Role.HOSPITAL);
        hospUser1.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        hospUser1.setEnabled(true);
        hospUser1 = userRepository.save(hospUser1);

        hospital1 = new Hospital();
        hospital1.setUser(hospUser1);
        hospital1.setHospitalName("General Hospital Lahore");
        hospital1.setRegistrationNumber("HOSP-LHR-" + suffix1);
        hospital1.setProvince(punjab);
        hospital1.setCity(lahore);
        hospital1.setCompleteAddress("Jail Road, Lahore");
        hospital1.setContactNumber(hospUser1.getPhoneNumber());
        hospital1.setVerified(false);
        hospital1 = hospitalRepository.save(hospital1);

        // Hospital 2 (Verified)
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        hospUser2 = new User();
        hospUser2.setFullName("Aga Khan Hospital");
        hospUser2.setEmail("agakhan." + suffix2 + "@hospital.org");
        hospUser2.setPhoneNumber("021-3" + suffix2.substring(0, 6));
        hospUser2.setPasswordHash("$2a$10$hashedpass");
        hospUser2.setRole(Role.HOSPITAL);
        hospUser2.setAccountStatus(AccountStatus.ACTIVE);
        hospUser2.setEnabled(true);
        hospUser2 = userRepository.save(hospUser2);

        hospital2 = new Hospital();
        hospital2.setUser(hospUser2);
        hospital2.setHospitalName("Aga Khan University Hospital");
        hospital2.setRegistrationNumber("HOSP-KHI-" + suffix2);
        hospital2.setProvince(sindh);
        hospital2.setCity(karachi);
        hospital2.setCompleteAddress("Stadium Road, Karachi");
        hospital2.setContactNumber(hospUser2.getPhoneNumber());
        hospital2.setVerified(true);
        hospital2 = hospitalRepository.save(hospital2);

        // Admin User
        adminUser = new User();
        adminUser.setFullName("System Admin");
        adminUser.setEmail("admin." + suffix1 + "@donornexa.com");
        adminUser.setPhoneNumber("0300-9" + suffix1.substring(0, 6));
        adminUser.setPasswordHash("$2a$10$hashedpass");
        adminUser.setRole(Role.ADMIN);
        adminUser.setAccountStatus(AccountStatus.ACTIVE);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // 1. Hospital Profile Retrieval
    // =========================================================================

    @Test
    @DisplayName("1. Hospital can view its own profile by authenticated email")
    void getHospitalProfile_success() {
        HospitalProfileDTO profile = hospitalService.getHospitalProfile(hospUser1.getEmail());

        assertThat(profile).isNotNull();
        assertThat(profile.getHospitalName()).isEqualTo("General Hospital Lahore");
        assertThat(profile.getRegistrationNumber()).isEqualTo(hospital1.getRegistrationNumber());
        assertThat(profile.getEmail()).isEqualTo(hospUser1.getEmail());
        assertThat(profile.getProvinceName()).isEqualTo("Punjab");
        assertThat(profile.getCityName()).isEqualTo("Lahore");
        assertThat(profile.getVerificationStatus()).isEqualTo(HospitalVerificationStatus.PENDING);
        assertThat(profile.getAccountStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("2. Missing hospital profile throws HospitalNotFoundException")
    void getHospitalProfile_notFound_throwsException() {
        assertThatThrownBy(() -> hospitalService.getHospitalProfile("nonexistent@hospital.org"))
                .isInstanceOf(HospitalNotFoundException.class)
                .hasMessageContaining("No hospital profile found");
    }

    // =========================================================================
    // 2. Permitted Profile Updates & Protected Fields
    // =========================================================================

    @Test
    @DisplayName("3. Hospital can update permitted fields (name, phone, location, address)")
    void updateHospitalProfile_permittedFields_success() {
        HospitalProfileUpdateDTO updateDTO = new HospitalProfileUpdateDTO();
        updateDTO.setHospitalName("Lahore General Hospital Complex");
        updateDTO.setContactNumber("042-39999999");
        updateDTO.setProvinceId(sindh.getId());
        updateDTO.setCityId(karachi.getId());
        updateDTO.setCompleteAddress("Clifton Block 2, Karachi");

        HospitalProfileDTO updated = hospitalService.updateHospitalProfile(hospUser1.getEmail(), updateDTO);

        assertThat(updated.getHospitalName()).isEqualTo("Lahore General Hospital Complex");
        assertThat(updated.getContactNumber()).isEqualTo("042-39999999");
        assertThat(updated.getProvinceName()).isEqualTo("Sindh");
        assertThat(updated.getCityName()).isEqualTo("Karachi");
        assertThat(updated.getCompleteAddress()).isEqualTo("Clifton Block 2, Karachi");

        // Verify entity persistence
        Hospital fromDb = hospitalRepository.findById(hospital1.getId()).orElseThrow();
        assertThat(fromDb.getHospitalName()).isEqualTo("Lahore General Hospital Complex");
        assertThat(fromDb.getContactNumber()).isEqualTo("042-39999999");
        assertThat(fromDb.getUser().getFullName()).isEqualTo("Lahore General Hospital Complex");
        assertThat(fromDb.getUser().getPhoneNumber()).isEqualTo("042-39999999");
    }

    @Test
    @DisplayName("4. Protected immutable fields remain completely untouched after profile update")
    void updateHospitalProfile_preservesImmutableFields() {
        String originalRegistrationNumber = hospital1.getRegistrationNumber();
        String originalEmail = hospUser1.getEmail();
        Boolean originalVerified = hospital1.getVerified();
        AccountStatus originalStatus = hospUser1.getAccountStatus();

        HospitalProfileUpdateDTO updateDTO = new HospitalProfileUpdateDTO();
        updateDTO.setHospitalName("Renamed Hospital");
        updateDTO.setContactNumber("042-38888888");
        updateDTO.setProvinceId(punjab.getId());
        updateDTO.setCityId(lahore.getId());
        updateDTO.setCompleteAddress("Updated Address");

        hospitalService.updateHospitalProfile(hospUser1.getEmail(), updateDTO);

        Hospital fromDb = hospitalRepository.findByIdWithDetails(hospital1.getId()).orElseThrow();
        assertThat(fromDb.getRegistrationNumber()).isEqualTo(originalRegistrationNumber);
        assertThat(fromDb.getUser().getEmail()).isEqualTo(originalEmail);
        assertThat(fromDb.getVerified()).isEqualTo(originalVerified);
        assertThat(fromDb.getUser().getAccountStatus()).isEqualTo(originalStatus);
        assertThat(fromDb.getUser().getRole()).isEqualTo(Role.HOSPITAL);
    }

    @Test
    @DisplayName("5. Profile update rejects duplicate phone number belonging to another user")
    void updateHospitalProfile_duplicatePhone_rejected() {
        HospitalProfileUpdateDTO updateDTO = new HospitalProfileUpdateDTO();
        updateDTO.setHospitalName("General Hospital Lahore");
        updateDTO.setContactNumber(hospUser2.getPhoneNumber()); // conflict with hospital 2
        updateDTO.setProvinceId(punjab.getId());
        updateDTO.setCityId(lahore.getId());
        updateDTO.setCompleteAddress("Jail Road, Lahore");

        assertThatThrownBy(() -> hospitalService.updateHospitalProfile(hospUser1.getEmail(), updateDTO))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("6. Profile update rejects mismatched city and province")
    void updateHospitalProfile_mismatchedCityAndProvince_rejected() {
        HospitalProfileUpdateDTO updateDTO = new HospitalProfileUpdateDTO();
        updateDTO.setHospitalName("General Hospital Lahore");
        updateDTO.setContactNumber("042-31111111");
        updateDTO.setProvinceId(punjab.getId()); // Punjab
        updateDTO.setCityId(karachi.getId());     // Karachi belongs to Sindh!
        updateDTO.setCompleteAddress("Test Address");

        assertThatThrownBy(() -> hospitalService.updateHospitalProfile(hospUser1.getEmail(), updateDTO))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("does not belong to province");
    }

    // =========================================================================
    // 3. Hospital Dashboard Aggregation
    // =========================================================================

    @Test
    @DisplayName("7. Hospital dashboard summary aggregates real database fields")
    void getDashboardSummary_success() {
        HospitalDashboardSummary summary = hospitalService.getDashboardSummary(hospUser1.getEmail());

        assertThat(summary).isNotNull();
        assertThat(summary.getHospitalName()).isEqualTo("General Hospital Lahore");
        assertThat(summary.getRegistrationNumber()).isEqualTo(hospital1.getRegistrationNumber());
        assertThat(summary.isProfileComplete()).isTrue();
        assertThat(summary.getVerificationStatus()).isEqualTo(HospitalVerificationStatus.PENDING);
        assertThat(summary.getAccountStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    // =========================================================================
    // 4. Administrator Verification Decisions (Approve & Reject)
    // =========================================================================

    @Test
    @DisplayName("8. Administrator can approve hospital verification (sets verified=true, account=ACTIVE)")
    void verifyHospital_approve_success() {
        HospitalVerificationActionDTO actionDTO = new HospitalVerificationActionDTO();
        actionDTO.setAction("APPROVE");

        Hospital verified = verificationService.verifyHospital(hospital1.getId(), actionDTO, adminUser.getEmail());

        assertThat(verified.getVerified()).isTrue();
        assertThat(verified.getRejectionReason()).isNull();
        assertThat(verified.getUser().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(verified.getUser().getEnabled()).isTrue();
        assertThat(verified.getVerificationStatus()).isEqualTo(HospitalVerificationStatus.VERIFIED);

        // Verify audit log recorded
        boolean auditFound = auditLogRepository.findByAction(AuditAction.VERIFY).stream()
                .anyMatch(a -> a.getEntityId().equals(String.valueOf(hospital1.getId())) && a.getDescription().contains("APPROVED"));
        assertThat(auditFound).isTrue();
    }

    @Test
    @DisplayName("9. Administrator can reject hospital verification with mandatory reason")
    void verifyHospital_reject_success() {
        HospitalVerificationActionDTO actionDTO = new HospitalVerificationActionDTO();
        actionDTO.setAction("REJECT");
        actionDTO.setRejectionReason("Registration number not found in Punjab Healthcare Commission registry.");

        Hospital rejected = verificationService.verifyHospital(hospital1.getId(), actionDTO, adminUser.getEmail());

        assertThat(rejected.getVerified()).isFalse();
        assertThat(rejected.getRejectionReason()).isEqualTo("Registration number not found in Punjab Healthcare Commission registry.");
        assertThat(rejected.getUser().getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(rejected.getUser().getEnabled()).isFalse();
        assertThat(rejected.getVerificationStatus()).isEqualTo(HospitalVerificationStatus.REJECTED);

        // Verify audit log recorded
        boolean auditFound = auditLogRepository.findByAction(AuditAction.VERIFY).stream()
                .anyMatch(a -> a.getEntityId().equals(String.valueOf(hospital1.getId())) && a.getDescription().contains("REJECTED"));
        assertThat(auditFound).isTrue();
    }

    @Test
    @DisplayName("10. Rejecting hospital verification without reason throws exception")
    void verifyHospital_rejectWithoutReason_throwsException() {
        HospitalVerificationActionDTO actionDTO = new HospitalVerificationActionDTO();
        actionDTO.setAction("REJECT");
        actionDTO.setRejectionReason(""); // Empty reason

        assertThatThrownBy(() -> verificationService.verifyHospital(hospital1.getId(), actionDTO, adminUser.getEmail()))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("rejection reason is required");
    }

    // =========================================================================
    // 5. Administrator Account Status Transitions
    // =========================================================================

    @Test
    @DisplayName("11. Administrator can suspend hospital account (accountStatus=SUSPENDED, enabled=false)")
    void updateAccountStatus_suspend_success() {
        HospitalAccountStatusActionDTO statusDTO = new HospitalAccountStatusActionDTO();
        statusDTO.setAccountStatus(AccountStatus.SUSPENDED);

        Hospital suspended = verificationService.updateAccountStatus(hospital2.getId(), statusDTO, adminUser.getEmail());

        assertThat(suspended.getUser().getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(suspended.getUser().getEnabled()).isFalse();

        // Verify audit log recorded
        boolean auditFound = auditLogRepository.findByAction(AuditAction.STATUS_CHANGE).stream()
                .anyMatch(a -> a.getEntityId().equals(String.valueOf(hospital2.getId())) && a.getDescription().contains("SUSPENDED"));
        assertThat(auditFound).isTrue();
    }

    // =========================================================================
    // 6. Search and Pagination
    // =========================================================================

    @Test
    @DisplayName("12. Search and filter by name and status returns matching hospitals")
    void searchHospitals_filtersCorrectly() {
        Page<Hospital> results = verificationService.searchHospitals(
                "Aga Khan",
                HospitalVerificationStatus.VERIFIED,
                AccountStatus.ACTIVE,
                PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().getFirst().getHospitalName()).contains("Aga Khan");
    }
}
