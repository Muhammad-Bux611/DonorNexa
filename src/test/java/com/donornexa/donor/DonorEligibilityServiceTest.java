package com.donornexa.donor;

import com.donornexa.common.enums.Role;
import com.donornexa.donor.dto.DonorEligibilityResult;
import com.donornexa.donor.service.DonorEligibilityService;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 4 â€” Unit Tests for {@link DonorEligibilityService}.
 * Tests the university CEP confirmed health and operational eligibility rules.
 */
@DisplayName("Phase 4 â€” Donor Eligibility Service Tests")
class DonorEligibilityServiceTest {

    private DonorEligibilityService eligibilityService;
    private Donor validDonor;
    private User validUser;

    @BeforeEach
    void setUp() {
        eligibilityService = new DonorEligibilityService();

        validUser = new User();
        validUser.setId(1L);
        validUser.setFullName("Ali Khan");
        validUser.setEmail("ali@example.com");
        validUser.setPhoneNumber("0300-1111111");
        validUser.setRole(Role.DONOR);
        validUser.setAccountStatus(AccountStatus.ACTIVE);
        validUser.setEnabled(true);

        Province province = new Province();
        province.setId(1L);
        province.setName("Punjab");

        City city = new City();
        city.setId(1L);
        city.setName("Lahore");
        city.setProvince(province);

        validDonor = new Donor();
        validDonor.setId(10L);
        validDonor.setUser(validUser);
        validDonor.setBloodGroup(BloodGroup.O_POSITIVE);
        validDonor.setGender(Gender.MALE);
        validDonor.setDateOfBirth(LocalDate.now().minusYears(25)); // 25 years old
        validDonor.setWeight(BigDecimal.valueOf(65.0));            // 65 kg
        validDonor.setProvince(province);
        validDonor.setCity(city);
        validDonor.setAvailability(DonorAvailability.AVAILABLE);
        validDonor.setVerified(true);
        validDonor.setLastDonationDate(null);                      // First time donor
    }

    @Test
    @DisplayName("1. Fully eligible donor satisfies all criteria")
    void fullyEligibleDonor_isEligible() {
        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.isAgeEligible()).isTrue();
        assertThat(result.isWeightEligible()).isTrue();
        assertThat(result.isCooldownEligible()).isTrue();
        assertThat(result.isAvailabilityEligible()).isTrue();
        assertThat(result.isAccountEligible()).isTrue();
        assertThat(result.getAge()).isEqualTo(25);
        assertThat(result.getStatusBadgeClass()).isEqualTo("bg-success");
    }

    @Test
    @DisplayName("2. Age below 18 is ineligible")
    void ageUnder18_isIneligible() {
        validDonor.setDateOfBirth(LocalDate.now().minusYears(17)); // 17 years old

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isAgeEligible()).isFalse();
        assertThat(result.getCriteriaMessages()).anyMatch(m -> m.contains("at least 18 years old"));
    }

    @Test
    @DisplayName("3. Age above 60 is ineligible")
    void ageOver60_isIneligible() {
        validDonor.setDateOfBirth(LocalDate.now().minusYears(61)); // 61 years old

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isAgeEligible()).isFalse();
        assertThat(result.getCriteriaMessages()).anyMatch(m -> m.contains("upper age limit"));
    }

    @Test
    @DisplayName("4. Weight below 50.0 kg is ineligible")
    void weightUnder50_isIneligible() {
        validDonor.setWeight(BigDecimal.valueOf(48.5));

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isWeightEligible()).isFalse();
        assertThat(result.getCriteriaMessages()).anyMatch(m -> m.contains("Minimum required weight is 50.0 kg"));
    }

    @Test
    @DisplayName("5. Unrecorded weight is marked pending and ineligible")
    void unrecordedWeight_isIneligible() {
        validDonor.setWeight(null);

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isWeightEligible()).isFalse();
        assertThat(result.getCriteriaMessages()).anyMatch(m -> m.contains("Body weight is unrecorded"));
    }

    @Test
    @DisplayName("6. Active 90-day cooldown interval makes donor temporarily ineligible")
    void activeCooldown_isIneligible() {
        // Last donation was 30 days ago
        LocalDate lastDonation = LocalDate.now().minusDays(30);
        validDonor.setLastDonationDate(lastDonation);

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isCooldownEligible()).isFalse();
        assertThat(result.getDaysSinceLastDonation()).isEqualTo(30L);
        assertThat(result.getDaysRemainingCooldown()).isEqualTo(60L);
        assertThat(result.getNextEligibleDate()).isEqualTo(lastDonation.plusDays(90));
        assertThat(result.getStatusBadgeClass()).contains("bg-info");
    }

    @Test
    @DisplayName("7. Cooldown satisfied after 90 days")
    void cooldownSatisfied_isEligible() {
        // Last donation was 91 days ago
        validDonor.setLastDonationDate(LocalDate.now().minusDays(91));

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.isCooldownEligible()).isTrue();
        assertThat(result.getDaysSinceLastDonation()).isEqualTo(91L);
        assertThat(result.getDaysRemainingCooldown()).isEqualTo(0L);
    }

    @Test
    @DisplayName("8. Donor marked TEMPORARILY_UNAVAILABLE is operationally ineligible")
    void temporarilyUnavailable_isIneligible() {
        validDonor.setAvailability(DonorAvailability.TEMPORARILY_UNAVAILABLE);

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isAvailabilityEligible()).isFalse();
        assertThat(result.isAgeEligible()).isTrue();
        assertThat(result.isWeightEligible()).isTrue();
        assertThat(result.getStatusBadgeClass()).contains("bg-warning");
    }

    @Test
    @DisplayName("9. Donor marked UNAVAILABLE is operationally ineligible")
    void unavailable_isIneligible() {
        validDonor.setAvailability(DonorAvailability.UNAVAILABLE);

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isAvailabilityEligible()).isFalse();
    }

    @Test
    @DisplayName("10. Suspended user account is ineligible")
    void suspendedAccount_isIneligible() {
        validUser.setAccountStatus(AccountStatus.SUSPENDED);

        DonorEligibilityResult result = eligibilityService.evaluateEligibility(validDonor);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.isAccountEligible()).isFalse();
    }

    @Test
    @DisplayName("11. Null donor throws IllegalArgumentException")
    void nullDonor_throwsException() {
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
