package com.donornexa.donor;

import com.donornexa.common.enums.Role;
import com.donornexa.donor.dto.DonorDashboardSummary;
import com.donornexa.donor.dto.DonorProfileUpdateRequest;
import com.donornexa.common.exception.DonorProfileException;
import com.donornexa.donor.service.DonorEligibilityService;
import com.donornexa.donor.service.DonorService;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 â€” Data JPA Integration Tests for {@link DonorService}.
 * Verifies profile retrieval, permitted updates, immutable field protection,
 * phone uniqueness, location consistency, availability toggles, and donation isolation.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Phase 4 â€” Donor Service Tests")
class DonorServiceTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private DonorService donorService;
    @Autowired private EntityManager em;

    private Province punjab;
    private Province sindh;
    private City lahore;
    private City karachi;
    private User user1;
    private User user2;
    private Donor donor1;
    private Donor donor2;
    private Hospital hospital;

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

        // Donor 1
        user1 = new User();
        user1.setFullName("Usman Tariq");
        user1.setEmail("usman@example.com");
        user1.setPhoneNumber("0300-1111111");
        user1.setPasswordHash("$2a$10$hashedpass");
        user1.setRole(Role.DONOR);
        user1.setAccountStatus(AccountStatus.ACTIVE);
        user1.setEnabled(true);
        user1 = userRepository.save(user1);

        donor1 = new Donor();
        donor1.setUser(user1);
        donor1.setBloodGroup(BloodGroup.O_POSITIVE);
        donor1.setGender(Gender.MALE);
        donor1.setDateOfBirth(LocalDate.of(1996, 4, 12));
        donor1.setWeight(BigDecimal.valueOf(70.0));
        donor1.setProvince(punjab);
        donor1.setCity(lahore);
        donor1.setCompleteAddress("Model Town, Lahore");
        donor1.setAvailability(DonorAvailability.AVAILABLE);
        donor1.setVerified(false);
        donor1 = donorRepository.save(donor1);

        // Donor 2 (for cross-user isolation tests)
        user2 = new User();
        user2.setFullName("Bilal Ahmed");
        user2.setEmail("bilal@example.com");
        user2.setPhoneNumber("0300-2222222");
        user2.setPasswordHash("$2a$10$hashedpass");
        user2.setRole(Role.DONOR);
        user2.setAccountStatus(AccountStatus.ACTIVE);
        user2.setEnabled(true);
        user2 = userRepository.save(user2);

        donor2 = new Donor();
        donor2.setUser(user2);
        donor2.setBloodGroup(BloodGroup.A_POSITIVE);
        donor2.setGender(Gender.MALE);
        donor2.setDateOfBirth(LocalDate.of(1998, 9, 25));
        donor2.setWeight(BigDecimal.valueOf(62.0));
        donor2.setProvince(sindh);
        donor2.setCity(karachi);
        donor2.setAvailability(DonorAvailability.AVAILABLE);
        donor2.setVerified(true);
        donor2 = donorRepository.save(donor2);

        // Hospital for donation test
        User hospUser = new User();
        hospUser.setFullName("Mayo Hospital");
        hospUser.setEmail("mayo@example.com");
        hospUser.setPhoneNumber("042-99200111");
        hospUser.setPasswordHash("$2a$10$hashedpass");
        hospUser.setRole(Role.HOSPITAL);
        hospUser.setAccountStatus(AccountStatus.ACTIVE);
        hospUser = userRepository.save(hospUser);

        hospital = new Hospital();
        hospital.setUser(hospUser);
        hospital.setHospitalName("Mayo Hospital Lahore");
        hospital.setRegistrationNumber("HOSP-LHR-001");
        hospital.setProvince(punjab);
        hospital.setCity(lahore);
        hospital.setContactNumber("042-99200111");
        hospital = hospitalRepository.save(hospital);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. Donor can view their own profile by email")
    void getDonorByUserEmail_success() {
        Donor found = donorService.getDonorByUserEmail("usman@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getUser().getEmail()).isEqualTo("usman@example.com");
        assertThat(found.getUser().getFullName()).isEqualTo("Usman Tariq");
        assertThat(found.getBloodGroup()).isEqualTo(BloodGroup.O_POSITIVE);
        assertThat(found.getProvince().getName()).isEqualTo("Punjab");
        assertThat(found.getCity().getName()).isEqualTo("Lahore");
    }

    @Test
    @DisplayName("2. Non-existent donor email throws DonorProfileException")
    void getDonorByUserEmail_notFound_throws() {
        assertThatThrownBy(() -> donorService.getDonorByUserEmail("unknown@example.com"))
                .isInstanceOf(DonorProfileException.class)
                .hasMessageContaining("User account not found");
    }

    @Test
    @DisplayName("3. Donor can update permitted profile fields")
    void updateProfile_success() {
        DonorProfileUpdateRequest request = new DonorProfileUpdateRequest();
        request.setFullName("Usman Tariq Updated");
        request.setPhoneNumber("0300-3333333");
        request.setWeight(BigDecimal.valueOf(74.5));
        request.setProvinceId(punjab.getId());
        request.setCityId(lahore.getId());
        request.setCompleteAddress("Gulberg III, Lahore");

        donorService.updateProfile("usman@example.com", request);

        em.flush();
        em.clear();

        Donor updated = donorService.getDonorByUserEmail("usman@example.com");
        assertThat(updated.getUser().getFullName()).isEqualTo("Usman Tariq Updated");
        assertThat(updated.getUser().getPhoneNumber()).isEqualTo("0300-3333333");
        assertThat(updated.getWeight()).isEqualByComparingTo(BigDecimal.valueOf(74.5));
        assertThat(updated.getCompleteAddress()).isEqualTo("Gulberg III, Lahore");
    }

    @Test
    @DisplayName("4. Updating profile protects immutable fields (role, verified, bloodGroup, DOB, email)")
    void updateProfile_protectsImmutableFields() {
        DonorProfileUpdateRequest request = new DonorProfileUpdateRequest();
        request.setFullName("Usman Tariq NewName");
        request.setPhoneNumber("0300-1111111");
        request.setWeight(BigDecimal.valueOf(72.0));
        request.setProvinceId(punjab.getId());
        request.setCityId(lahore.getId());

        donorService.updateProfile("usman@example.com", request);

        em.flush();
        em.clear();

        Donor updated = donorService.getDonorByUserEmail("usman@example.com");
        assertThat(updated.getUser().getRole()).isEqualTo(Role.DONOR);
        assertThat(updated.getVerified()).isFalse();
        assertThat(updated.getUser().getEmail()).isEqualTo("usman@example.com");
        assertThat(updated.getBloodGroup()).isEqualTo(BloodGroup.O_POSITIVE);
        assertThat(updated.getDateOfBirth()).isEqualTo(LocalDate.of(1996, 4, 12));
    }

    @Test
    @DisplayName("5. Updating profile with a phone number belonging to another user throws DonorProfileException")
    void updateProfile_duplicatePhone_throws() {
        DonorProfileUpdateRequest request = new DonorProfileUpdateRequest();
        request.setFullName("Usman Tariq");
        // Try using user2's phone number
        request.setPhoneNumber("0300-2222222");
        request.setProvinceId(punjab.getId());
        request.setCityId(lahore.getId());

        assertThatThrownBy(() -> donorService.updateProfile("usman@example.com", request))
                .isInstanceOf(DonorProfileException.class)
                .hasMessageContaining("already in use by another account");
    }

    @Test
    @DisplayName("6. Updating profile with city not belonging to province throws DonorProfileException")
    void updateProfile_mismatchedLocation_throws() {
        DonorProfileUpdateRequest request = new DonorProfileUpdateRequest();
        request.setFullName("Usman Tariq");
        request.setPhoneNumber("0300-1111111");
        // Mismatch: Punjab province with Karachi (Sindh) city
        request.setProvinceId(punjab.getId());
        request.setCityId(karachi.getId());

        assertThatThrownBy(() -> donorService.updateProfile("usman@example.com", request))
                .isInstanceOf(DonorProfileException.class)
                .hasMessageContaining("does not belong to");
    }

    @Test
    @DisplayName("7. Donor can update operational availability status")
    void updateAvailability_success() {
        donorService.updateAvailability("usman@example.com", DonorAvailability.TEMPORARILY_UNAVAILABLE);

        em.flush();
        em.clear();

        Donor updated = donorService.getDonorByUserEmail("usman@example.com");
        assertThat(updated.getAvailability()).isEqualTo(DonorAvailability.TEMPORARILY_UNAVAILABLE);

        donorService.updateAvailability("usman@example.com", DonorAvailability.AVAILABLE);

        em.flush();
        em.clear();

        updated = donorService.getDonorByUserEmail("usman@example.com");
        assertThat(updated.getAvailability()).isEqualTo(DonorAvailability.AVAILABLE);
    }

    @Test
    @DisplayName("8. Donation history displays only the authenticated donor's records")
    void getDonationHistory_returnsOnlyOwnDonations() {
        // Create 2 donations for donor1
        Donation d1 = new Donation();
        d1.setDonor(donor1);
        d1.setHospital(hospital);
        d1.setUnitsDonated(1);
        d1.setDonationDate(LocalDateTime.now().minusDays(10));
        d1.setDonationStatus(DonationStatus.COMPLETED);
        donationRepository.save(d1);

        Donation d2 = new Donation();
        d2.setDonor(donor1);
        d2.setHospital(hospital);
        d2.setUnitsDonated(1);
        d2.setDonationDate(LocalDateTime.now().minusDays(5));
        d2.setDonationStatus(DonationStatus.SCHEDULED);
        donationRepository.save(d2);

        // Create 1 donation for donor2
        Donation d3 = new Donation();
        d3.setDonor(donor2);
        d3.setHospital(hospital);
        d3.setUnitsDonated(2);
        d3.setDonationDate(LocalDateTime.now().minusDays(2));
        d3.setDonationStatus(DonationStatus.COMPLETED);
        donationRepository.save(d3);

        em.flush();
        em.clear();

        List<Donation> donor1Donations = donorService.getDonationHistory("usman@example.com");
        List<Donation> donor2Donations = donorService.getDonationHistory("bilal@example.com");

        assertThat(donor1Donations).hasSize(2);
        assertThat(donor1Donations).allMatch(d -> d.getDonor().getId().equals(donor1.getId()));
        // Ordered by date desc
        assertThat(donor1Donations.get(0).getDonationDate()).isAfter(donor1Donations.get(1).getDonationDate());

        assertThat(donor2Donations).hasSize(1);
        assertThat(donor2Donations.get(0).getDonor().getId().equals(donor2.getId()));
    }

    @Test
    @DisplayName("9. Empty donation history returns empty list")
    void getDonationHistory_empty_returnsEmptyList() {
        List<Donation> donations = donorService.getDonationHistory("usman@example.com");
        assertThat(donations).isEmpty();
    }

    @Test
    @DisplayName("10. Dashboard summary computes actual database statistics")
    void getDashboardSummary_computesActualStatistics() {
        Donation donation = new Donation();
        donation.setDonor(donor1);
        donation.setHospital(hospital);
        donation.setUnitsDonated(2);
        donation.setDonationDate(LocalDateTime.now().minusDays(15));
        donation.setDonationStatus(DonationStatus.COMPLETED);
        donationRepository.save(donation);

        em.flush();
        em.clear();

        DonorDashboardSummary summary = donorService.getDashboardSummary("usman@example.com");

        assertThat(summary.getTotalDonationsCount()).isEqualTo(1);
        assertThat(summary.getCompletedDonationsCount()).isEqualTo(1);
        assertThat(summary.getTotalUnitsDonated()).isEqualTo(2);
        assertThat(summary.getRecentDonations()).hasSize(1);
        assertThat(summary.getEligibility()).isNotNull();
        assertThat(summary.getProfileCompletionPercentage()).isEqualTo(100);
    }
}
