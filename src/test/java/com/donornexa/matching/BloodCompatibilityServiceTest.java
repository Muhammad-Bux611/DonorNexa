package com.donornexa.matching;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.matching.service.BloodCompatibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 7 â€” Blood Compatibility Service Unit Tests")
class BloodCompatibilityServiceTest {

    private BloodCompatibilityService compatibilityService;

    @BeforeEach
    void setUp() {
        compatibilityService = new BloodCompatibilityService();
    }

    // =========================================================================
    // 1. Universal Donor (O-) Tests
    // =========================================================================
    @Test
    @DisplayName("O- is universal red cell donor: compatible with all 8 blood groups")
    void oNegative_compatibleWithAllRecipients() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertThat(compatibilityService.isCompatible(BloodGroup.O_NEGATIVE, recipient))
                    .as("O- must be compatible with recipient %s", recipient)
                    .isTrue();
        }
    }

    // =========================================================================
    // 2. Universal Recipient (AB+) Tests
    // =========================================================================
    @Test
    @DisplayName("AB+ is universal red cell recipient: can receive from all 8 blood groups")
    void abPositive_canReceiveFromAllDonors() {
        for (BloodGroup donor : BloodGroup.values()) {
            assertThat(compatibilityService.isCompatible(donor, BloodGroup.AB_POSITIVE))
                    .as("AB+ recipient must be able to receive from donor %s", donor)
                    .isTrue();
        }
    }

    // =========================================================================
    // 3. Strict ABO/Rh Red Blood Cell Compatibility Matrix (All 64 combos)
    // =========================================================================
    @ParameterizedTest(name = "Donor {0} to Recipient {1} -> Expected {2}")
    @CsvSource({
            // O- Donor (8/8 compatible)
            "O_NEGATIVE, O_NEGATIVE, true",
            "O_NEGATIVE, O_POSITIVE, true",
            "O_NEGATIVE, A_NEGATIVE, true",
            "O_NEGATIVE, A_POSITIVE, true",
            "O_NEGATIVE, B_NEGATIVE, true",
            "O_NEGATIVE, B_POSITIVE, true",
            "O_NEGATIVE, AB_NEGATIVE, true",
            "O_NEGATIVE, AB_POSITIVE, true",

            // O+ Donor (4/8 compatible)
            "O_POSITIVE, O_NEGATIVE, false",
            "O_POSITIVE, O_POSITIVE, true",
            "O_POSITIVE, A_NEGATIVE, false",
            "O_POSITIVE, A_POSITIVE, true",
            "O_POSITIVE, B_NEGATIVE, false",
            "O_POSITIVE, B_POSITIVE, true",
            "O_POSITIVE, AB_NEGATIVE, false",
            "O_POSITIVE, AB_POSITIVE, true",

            // A- Donor (4/8 compatible)
            "A_NEGATIVE, O_NEGATIVE, false",
            "A_NEGATIVE, O_POSITIVE, false",
            "A_NEGATIVE, A_NEGATIVE, true",
            "A_NEGATIVE, A_POSITIVE, true",
            "A_NEGATIVE, B_NEGATIVE, false",
            "A_NEGATIVE, B_POSITIVE, false",
            "A_NEGATIVE, AB_NEGATIVE, true",
            "A_NEGATIVE, AB_POSITIVE, true",

            // A+ Donor (2/8 compatible)
            "A_POSITIVE, O_NEGATIVE, false",
            "A_POSITIVE, O_POSITIVE, false",
            "A_POSITIVE, A_NEGATIVE, false",
            "A_POSITIVE, A_POSITIVE, true",
            "A_POSITIVE, B_NEGATIVE, false",
            "A_POSITIVE, B_POSITIVE, false",
            "A_POSITIVE, AB_NEGATIVE, false",
            "A_POSITIVE, AB_POSITIVE, true",

            // B- Donor (4/8 compatible)
            "B_NEGATIVE, O_NEGATIVE, false",
            "B_NEGATIVE, O_POSITIVE, false",
            "B_NEGATIVE, A_NEGATIVE, false",
            "B_NEGATIVE, A_POSITIVE, false",
            "B_NEGATIVE, B_NEGATIVE, true",
            "B_NEGATIVE, B_POSITIVE, true",
            "B_NEGATIVE, AB_NEGATIVE, true",
            "B_NEGATIVE, AB_POSITIVE, true",

            // B+ Donor (2/8 compatible)
            "B_POSITIVE, O_NEGATIVE, false",
            "B_POSITIVE, O_POSITIVE, false",
            "B_POSITIVE, A_NEGATIVE, false",
            "B_POSITIVE, A_POSITIVE, false",
            "B_POSITIVE, B_NEGATIVE, false",
            "B_POSITIVE, B_POSITIVE, true",
            "B_POSITIVE, AB_NEGATIVE, false",
            "B_POSITIVE, AB_POSITIVE, true",

            // AB- Donor (2/8 compatible)
            "AB_NEGATIVE, O_NEGATIVE, false",
            "AB_NEGATIVE, O_POSITIVE, false",
            "AB_NEGATIVE, A_NEGATIVE, false",
            "AB_NEGATIVE, A_POSITIVE, false",
            "AB_NEGATIVE, B_NEGATIVE, false",
            "AB_NEGATIVE, B_POSITIVE, false",
            "AB_NEGATIVE, AB_NEGATIVE, true",
            "AB_NEGATIVE, AB_POSITIVE, true",

            // AB+ Donor (1/8 compatible)
            "AB_POSITIVE, O_NEGATIVE, false",
            "AB_POSITIVE, O_POSITIVE, false",
            "AB_POSITIVE, A_NEGATIVE, false",
            "AB_POSITIVE, A_POSITIVE, false",
            "AB_POSITIVE, B_NEGATIVE, false",
            "AB_POSITIVE, B_POSITIVE, false",
            "AB_POSITIVE, AB_NEGATIVE, false",
            "AB_POSITIVE, AB_POSITIVE, true"
    })
    @DisplayName("Verify red cell compatibility for all 64 combinations")
    void testFullCompatibilityMatrix(BloodGroup donor, BloodGroup recipient, boolean expected) {
        assertThat(compatibilityService.isCompatible(donor, recipient)).isEqualTo(expected);
    }

    // =========================================================================
    // 4. Compatible Donor Groups Set Queries
    // =========================================================================
    @Test
    @DisplayName("Recipient O- can only receive from O-")
    void recipientONegative_onlyReceivesFromONegative() {
        Set<BloodGroup> donors = compatibilityService.getCompatibleDonorGroups(BloodGroup.O_NEGATIVE);
        assertThat(donors).containsExactly(BloodGroup.O_NEGATIVE);
    }

    @Test
    @DisplayName("Recipient A+ can receive from O-, O+, A-, A+")
    void recipientAPositive_receivesFromFourGroups() {
        Set<BloodGroup> donors = compatibilityService.getCompatibleDonorGroups(BloodGroup.A_POSITIVE);
        assertThat(donors).containsExactlyInAnyOrder(
                BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE);
    }

    @Test
    @DisplayName("Recipient B+ can receive from O-, O+, B-, B+")
    void recipientBPositive_receivesFromFourGroups() {
        Set<BloodGroup> donors = compatibilityService.getCompatibleDonorGroups(BloodGroup.B_POSITIVE);
        assertThat(donors).containsExactlyInAnyOrder(
                BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE,
                BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE);
    }

    @Test
    @DisplayName("Recipient AB- can receive from O-, A-, B-, AB-")
    void recipientABNegative_receivesFromRhNegativeGroups() {
        Set<BloodGroup> donors = compatibilityService.getCompatibleDonorGroups(BloodGroup.AB_NEGATIVE);
        assertThat(donors).containsExactlyInAnyOrder(
                BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE,
                BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE);
    }

    // =========================================================================
    // 5. Exact Match and Descriptions
    // =========================================================================
    @Test
    @DisplayName("Exact match detection")
    void exactMatchDetection() {
        assertThat(compatibilityService.isExactMatch(BloodGroup.A_POSITIVE, BloodGroup.A_POSITIVE)).isTrue();
        assertThat(compatibilityService.isExactMatch(BloodGroup.O_NEGATIVE, BloodGroup.A_POSITIVE)).isFalse();
    }

    @Test
    @DisplayName("Null safety for compatibility checks")
    void nullSafety() {
        assertThat(compatibilityService.isCompatible(null, BloodGroup.A_POSITIVE)).isFalse();
        assertThat(compatibilityService.isCompatible(BloodGroup.A_POSITIVE, null)).isFalse();
        assertThat(compatibilityService.isCompatible(null, null)).isFalse();
        assertThat(compatibilityService.getCompatibleDonorGroups(null)).isEmpty();
    }
}
