package com.donornexa.admin;

import com.donornexa.admin.dto.AdminPasswordChangeDTO;
import com.donornexa.admin.dto.AdminProfileUpdateDTO;
import com.donornexa.admin.service.AdminDataSeeder;
import com.donornexa.admin.service.AdminProfileService;
import com.donornexa.admin.service.AdminReportService;
import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 9 â€” Administrator Profile, Password Security, Reports and Seeder Tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 9 â€” Admin Profile & Service Operations Tests")
class AdminProfileAndServiceTest {

    @Autowired private AdminProfileService profileService;
    @Autowired private AdminReportService reportService;
    @Autowired private AdminDataSeeder adminDataSeeder;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String ADMIN_TEST_EMAIL = "admin.profops@donornexa.com";
    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmail(ADMIN_TEST_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(ADMIN_TEST_EMAIL);
            u.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
            u.setFullName("Admin Prof Ops");
            u.setPhoneNumber("03008888881");
            u.setRole(Role.ADMIN);
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setEnabled(true);
            return userRepository.save(u);
        });

        // Reset password to known state
        adminUser.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("1. Administrator can successfully update their profile details")
    void admin_updateProfileDetails_success() {
        AdminProfileUpdateDTO dto = new AdminProfileUpdateDTO();
        dto.setFullName("Updated Lead Admin");
        dto.setPhoneNumber("03008888889");

        User updated = profileService.updateProfile(ADMIN_TEST_EMAIL, dto);
        assertEquals("Updated Lead Admin", updated.getFullName());
        assertEquals("03008888889", updated.getPhoneNumber());
    }

    @Test
    @DisplayName("2. Updating phone number to one already in use throws IllegalArgumentException")
    void admin_updateProfile_duplicatePhone_fails() {
        // Create another user with a specific phone
        User other = new User();
        other.setEmail("other.user.phone@donornexa.com");
        other.setPasswordHash(passwordEncoder.encode("Password123!"));
        other.setFullName("Other User");
        other.setPhoneNumber("03007777771");
        other.setRole(Role.DONOR);
        other.setAccountStatus(AccountStatus.ACTIVE);
        other.setEnabled(true);
        userRepository.save(other);

        AdminProfileUpdateDTO dto = new AdminProfileUpdateDTO();
        dto.setFullName("Admin Attempt");
        dto.setPhoneNumber("03007777771");

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile(ADMIN_TEST_EMAIL, dto));
    }

    @Test
    @DisplayName("3. Administrator changes password with correct current password â€” succeeds")
    void admin_changePassword_success() {
        AdminPasswordChangeDTO dto = new AdminPasswordChangeDTO();
        dto.setCurrentPassword("OldPassword123!");
        dto.setNewPassword("NewSecurePass456!");
        dto.setConfirmPassword("NewSecurePass456!");

        profileService.changePassword(ADMIN_TEST_EMAIL, dto);

        User updated = userRepository.findByEmail(ADMIN_TEST_EMAIL).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePass456!", updated.getPasswordHash()));
    }

    @Test
    @DisplayName("4. Changing password with incorrect current password throws IllegalArgumentException")
    void admin_changePassword_wrongCurrentPassword_fails() {
        AdminPasswordChangeDTO dto = new AdminPasswordChangeDTO();
        dto.setCurrentPassword("WrongCurrentPass!");
        dto.setNewPassword("NewSecurePass456!");
        dto.setConfirmPassword("NewSecurePass456!");

        assertThrows(IllegalArgumentException.class, () -> profileService.changePassword(ADMIN_TEST_EMAIL, dto));
    }

    @Test
    @DisplayName("5. Changing password with mismatching confirmation password throws IllegalArgumentException")
    void admin_changePassword_mismatchConfirmation_fails() {
        AdminPasswordChangeDTO dto = new AdminPasswordChangeDTO();
        dto.setCurrentPassword("OldPassword123!");
        dto.setNewPassword("NewSecurePass456!");
        dto.setConfirmPassword("CompletelyDifferentPass789!");

        assertThrows(IllegalArgumentException.class, () -> profileService.changePassword(ADMIN_TEST_EMAIL, dto));
    }

    @Test
    @DisplayName("6. Comprehensive reports service computes valid aggregate metrics")
    void reportService_computesValidMetrics() {
        Map<String, Object> report = reportService.generateComprehensiveReport();

        assertNotNull(report);
        assertTrue(report.containsKey("totalUsers"));
        assertTrue(report.containsKey("totalDonors"));
        assertTrue(report.containsKey("totalHospitals"));
        assertTrue(report.containsKey("totalRequests"));
        assertTrue(report.containsKey("totalDonations"));
        assertTrue(report.containsKey("totalInventoryUnits"));
        assertTrue(report.containsKey("donorBloodGroups"));
        assertTrue(report.containsKey("inventoryByGroup"));
    }

    @Test
    @DisplayName("7. AdminDataSeeder is idempotent when admin accounts already exist")
    void adminDataSeeder_idempotent() {
        long countBefore = userRepository.count();
        adminDataSeeder.run();
        long countAfter = userRepository.count();
        assertEquals(countBefore, countAfter);
    }
}
