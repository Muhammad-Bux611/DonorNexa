package com.donornexa.donor;

import com.donornexa.badge.entity.DonationBadge;
import com.donornexa.badge.service.DonationBadgeService;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.donation.repository.DonationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CEP Audit â€” Donor Milestone Badges Tests")
class DonationBadgeServiceTest {

    @Mock
    private DonationRepository donationRepository;

    private DonationBadgeService badgeService;

    @BeforeEach
    void setUp() {
        badgeService = new DonationBadgeService(donationRepository);
    }

    @Test
    @DisplayName("Prospective donor with 0 donations has no badges and 0% progress to Bronze")
    void shouldReturnProspectiveForZeroDonations() {
        when(donationRepository.countByDonorIdAndDonationStatus(1L, DonationStatus.COMPLETED)).thenReturn(0L);

        DonationBadgeService.DonorBadgeProfile profile = badgeService.getBadgeProfileForDonor(1L);

        assertThat(profile.getHighestBadge()).isNull();
        assertThat(profile.getEarnedBadges()).isEmpty();
        assertThat(profile.getNextBadge()).isEqualTo(DonationBadge.BRONZE_LIFESAVER);
        assertThat(profile.getProgressToNextBadgePercent()).isEqualTo(0);
    }

    @Test
    @DisplayName("Donor with 1 completed donation earns Bronze Lifesaver badge")
    void shouldEarnBronzeLifesaverBadge() {
        when(donationRepository.countByDonorIdAndDonationStatus(2L, DonationStatus.COMPLETED)).thenReturn(1L);

        DonationBadgeService.DonorBadgeProfile profile = badgeService.getBadgeProfileForDonor(2L);

        assertThat(profile.getHighestBadge()).isEqualTo(DonationBadge.BRONZE_LIFESAVER);
        assertThat(profile.getEarnedBadges()).containsExactly(DonationBadge.BRONZE_LIFESAVER);
        assertThat(profile.getNextBadge()).isEqualTo(DonationBadge.SILVER_GUARDIAN);
        assertThat(profile.getProgressToNextBadgePercent()).isEqualTo(20); // 1 out of 5 = 20%
    }

    @Test
    @DisplayName("Donor with 5 completed donations earns Silver Guardian badge")
    void shouldEarnSilverGuardianBadge() {
        when(donationRepository.countByDonorIdAndDonationStatus(3L, DonationStatus.COMPLETED)).thenReturn(5L);

        DonationBadgeService.DonorBadgeProfile profile = badgeService.getBadgeProfileForDonor(3L);

        assertThat(profile.getHighestBadge()).isEqualTo(DonationBadge.SILVER_GUARDIAN);
        assertThat(profile.getEarnedBadges()).containsExactly(DonationBadge.BRONZE_LIFESAVER, DonationBadge.SILVER_GUARDIAN);
        assertThat(profile.getNextBadge()).isEqualTo(DonationBadge.GOLD_HERO);
        assertThat(profile.getProgressToNextBadgePercent()).isEqualTo(50); // 5 out of 10 = 50%
    }

    @Test
    @DisplayName("Donor with 10+ completed donations earns Gold Hero badge")
    void shouldEarnGoldHeroBadge() {
        when(donationRepository.countByDonorIdAndDonationStatus(4L, DonationStatus.COMPLETED)).thenReturn(12L);

        DonationBadgeService.DonorBadgeProfile profile = badgeService.getBadgeProfileForDonor(4L);

        assertThat(profile.getHighestBadge()).isEqualTo(DonationBadge.GOLD_HERO);
        assertThat(profile.getEarnedBadges()).containsExactly(
                DonationBadge.BRONZE_LIFESAVER,
                DonationBadge.SILVER_GUARDIAN,
                DonationBadge.GOLD_HERO
        );
        assertThat(profile.getNextBadge()).isNull();
        assertThat(profile.getProgressToNextBadgePercent()).isEqualTo(100);
    }
}
