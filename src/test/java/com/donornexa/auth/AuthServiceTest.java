package com.donornexa.auth;

import com.donornexa.auth.dto.DonorRegistrationRequest;
import com.donornexa.auth.dto.HospitalRegistrationRequest;
import com.donornexa.common.exception.RegistrationException;
import com.donornexa.auth.service.AuthService;
import com.donornexa.auth.service.CustomUserDetailsService;
import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.Gender;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 3 â€” Authentication & Registration Tests
 *
 * <p>These tests run against the H2 in-memory database (test/resources/application.properties)
 * so no live MySQL connection is required. They cover:
 * <ul>
 *   <li>Donor and hospital registration flows</li>
 *   <li>Duplicate detection</li>
 *   <li>Password hashing</li>
 *   <li>CustomUserDetailsService loading and account-disabled logic</li>
 * </ul>
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({AuthService.class, CustomUserDetailsService.class, BCryptPasswordEncoder.class, com.donornexa.common.service.FileUploadService.class})
@DisplayName("Phase 3 â€” Authentication & Registration Tests")
class AuthServiceTest {

    @Autowired private UserRepository       userRepository;
    @Autowired private DonorRepository      donorRepository;
    @Autowired private HospitalRepository   hospitalRepository;
    @Autowired private ProvinceRepository   provinceRepository;
    @Autowired private CityRepository       cityRepository;
    @Autowired private AuthService          authService;
    @Autowired private CustomUserDetailsService customUserDetailsService;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private TestEntityManager    em;

    private Province province;
    private City     city;

    @BeforeEach
    void setUpLocationData() {
        province = new Province();
        province.setName("Test Province");
        province = provinceRepository.save(province);

        city = new City();
        city.setName("Test City");
        city.setProvince(province);
        city = cityRepository.save(city);

        em.flush();
        em.clear();
    }

    // =========================================================================
    // Helper builders
    // =========================================================================

    private DonorRegistrationRequest buildDonorRequest(String email, String phone) {
        DonorRegistrationRequest req = new DonorRegistrationRequest();
        req.setFullName("Test Donor");
        req.setEmail(email);
        req.setPhoneNumber(phone);
        req.setPassword("SecurePass1!");
        req.setConfirmPassword("SecurePass1!");
        req.setBloodGroup(BloodGroup.A_POSITIVE);
        req.setGender(Gender.MALE);
        req.setDateOfBirth(LocalDate.of(1990, 5, 15));
        req.setProvinceId(province.getId());
        req.setCityId(city.getId());
        req.setCompleteAddress("123 Test Street");
        return req;
    }

    private HospitalRegistrationRequest buildHospitalRequest(String email, String phone, String regNo) {
        HospitalRegistrationRequest req = new HospitalRegistrationRequest();
        req.setHospitalName("Test Hospital");
        req.setRegistrationNumber(regNo);
        req.setEmail(email);
        req.setPhoneNumber(phone);
        req.setPassword("SecurePass1!");
        req.setConfirmPassword("SecurePass1!");
        req.setProvinceId(province.getId());
        req.setCityId(city.getId());
        req.setCompleteAddress("456 Hospital Road");
        return req;
    }

    // =========================================================================
    // 1. Donor registration â€” success
    // =========================================================================

    @Test
    @DisplayName("1. Donor registration creates User and Donor records")
    void donorRegistration_success() {
        authService.registerDonor(buildDonorRequest("donor@test.com", "0300-1111111"));

        assertThat(userRepository.existsByEmail("donor@test.com")).isTrue();
        User user = userRepository.findByEmail("donor@test.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.DONOR);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
        assertThat(donorRepository.findByUserId(user.getId())).isPresent();
    }

    // =========================================================================
    // 2. Hospital registration â€” success
    // =========================================================================

    @Test
    @DisplayName("2. Hospital registration creates User and Hospital records")
    void hospitalRegistration_success() {
        authService.registerHospital(buildHospitalRequest("hosp@test.com", "021-1111111", "REG-H001"));

        assertThat(userRepository.existsByEmail("hosp@test.com")).isTrue();
        User user = userRepository.findByEmail("hosp@test.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.HOSPITAL);
        assertThat(hospitalRepository.findByUserId(user.getId())).isPresent();
    }

    // =========================================================================
    // 3. Duplicate email rejection
    // =========================================================================

    @Test
    @DisplayName("3. Duplicate email throws RegistrationException")
    void registration_duplicateEmail_throws() {
        authService.registerDonor(buildDonorRequest("dup@test.com", "0301-1111111"));

        DonorRegistrationRequest dup = buildDonorRequest("dup@test.com", "0302-2222222");
        assertThatThrownBy(() -> authService.registerDonor(dup))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("dup@test.com");
    }

    // =========================================================================
    // 4. Duplicate phone rejection
    // =========================================================================

    @Test
    @DisplayName("4. Duplicate phone number throws RegistrationException")
    void registration_duplicatePhone_throws() {
        authService.registerDonor(buildDonorRequest("phone1@test.com", "0303-9999999"));

        DonorRegistrationRequest dup = buildDonorRequest("phone2@test.com", "0303-9999999");
        assertThatThrownBy(() -> authService.registerDonor(dup))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("0303-9999999");
    }

    // =========================================================================
    // 5. Duplicate hospital registration number rejection
    // =========================================================================

    @Test
    @DisplayName("5. Duplicate hospital registration number throws RegistrationException")
    void registration_duplicateHospitalRegNo_throws() {
        authService.registerHospital(buildHospitalRequest("h1@test.com", "021-1111111", "SAME-REG"));

        HospitalRegistrationRequest dup = buildHospitalRequest("h2@test.com", "021-2222222", "SAME-REG");
        assertThatThrownBy(() -> authService.registerHospital(dup))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("SAME-REG");
    }

    // =========================================================================
    // 6. Password hashing â€” plain text is never stored
    // =========================================================================

    @Test
    @DisplayName("6. Password is BCrypt-hashed â€” plain text never stored")
    void registration_passwordIsHashed() {
        authService.registerDonor(buildDonorRequest("hash@test.com", "0304-1111111"));

        User user = userRepository.findByEmail("hash@test.com").orElseThrow();
        String storedHash = user.getPasswordHash();

        assertThat(storedHash).isNotEqualTo("SecurePass1!");
        assertThat(storedHash).startsWith("$2a$");   // BCrypt marker
        assertThat(passwordEncoder.matches("SecurePass1!", storedHash)).isTrue();
    }

    // =========================================================================
    // 7. Password mismatch
    // =========================================================================

    @Test
    @DisplayName("7. Password mismatch throws RegistrationException")
    void registration_passwordMismatch_throws() {
        DonorRegistrationRequest req = buildDonorRequest("mismatch@test.com", "0305-1111111");
        req.setConfirmPassword("DifferentPass1!");

        assertThatThrownBy(() -> authService.registerDonor(req))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("do not match");
    }

    // =========================================================================
    // 8. Successful login â€” UserDetailsService loads user
    // =========================================================================

    @Test
    @DisplayName("8. CustomUserDetailsService loads registered user by email")
    void userDetailsService_loadsUserByEmail() {
        authService.registerDonor(buildDonorRequest("login@test.com", "0306-1111111"));

        UserDetails details = customUserDetailsService.loadUserByUsername("login@test.com");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("login@test.com");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .anyMatch(a -> "ROLE_DONOR".equals(a.getAuthority()));
    }

    // =========================================================================
    // 9. Invalid login â€” unknown email
    // =========================================================================

    @Test
    @DisplayName("9. Loading unknown email throws UsernameNotFoundException")
    void userDetailsService_unknownEmail_throws() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nobody@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // =========================================================================
    // 10. Inactive account â€” disabled in UserDetails
    // =========================================================================

    @Test
    @DisplayName("10. INACTIVE account is reported as disabled")
    void userDetailsService_inactiveAccount_isDisabled() {
        authService.registerDonor(buildDonorRequest("inactive@test.com", "0307-1111111"));

        User user = userRepository.findByEmail("inactive@test.com").orElseThrow();
        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.saveAndFlush(user);
        em.clear();

        UserDetails details = customUserDetailsService.loadUserByUsername("inactive@test.com");
        assertThat(details.isEnabled()).isFalse();
    }

    // =========================================================================
    // 11. SUSPENDED account â€” disabled in UserDetails
    // =========================================================================

    @Test
    @DisplayName("11. SUSPENDED account is reported as disabled")
    void userDetailsService_suspendedAccount_isDisabled() {
        authService.registerHospital(buildHospitalRequest("suspended@test.com", "021-3333333", "SUSP-REG"));

        User user = userRepository.findByEmail("suspended@test.com").orElseThrow();
        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.saveAndFlush(user);
        em.clear();

        UserDetails details = customUserDetailsService.loadUserByUsername("suspended@test.com");
        assertThat(details.isEnabled()).isFalse();
    }

    // =========================================================================
    // 12. DONOR role authority
    // =========================================================================

    @Test
    @DisplayName("12. Donor has ROLE_DONOR authority")
    void donorRegistration_hasCorrectAuthority() {
        authService.registerDonor(buildDonorRequest("roledonor@test.com", "0308-1111111"));
        UserDetails details = customUserDetailsService.loadUserByUsername("roledonor@test.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DONOR");
    }

    // =========================================================================
    // 13. HOSPITAL role authority
    // =========================================================================

    @Test
    @DisplayName("13. Hospital has ROLE_HOSPITAL authority")
    void hospitalRegistration_hasCorrectAuthority() {
        authService.registerHospital(buildHospitalRequest("rolehosp@test.com", "021-4444444", "ROLE-H001"));
        UserDetails details = customUserDetailsService.loadUserByUsername("rolehosp@test.com");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_HOSPITAL");
    }

    // =========================================================================
    // 14. ADMIN role cannot be registered via public registration
    // =========================================================================

    @Test
    @DisplayName("14. Public registration always assigns DONOR role, never ADMIN")
    void donorRegistration_neverCreatesAdminRole() {
        authService.registerDonor(buildDonorRequest("notadmin@test.com", "0309-1111111"));

        User user = userRepository.findByEmail("notadmin@test.com").orElseThrow();
        assertThat(user.getRole()).isNotEqualTo(Role.ADMIN);
        assertThat(user.getRole()).isEqualTo(Role.DONOR);
    }

    // =========================================================================
    // 15. Donor profile is NOT verified on registration
    // =========================================================================

    @Test
    @DisplayName("15. Donor verification flag is false after registration")
    void donorRegistration_verifiedIsFalse() {
        authService.registerDonor(buildDonorRequest("verify@test.com", "0310-1111111"));

        User user = userRepository.findByEmail("verify@test.com").orElseThrow();
        var donor = donorRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(donor.getVerified()).isFalse();
    }
}
