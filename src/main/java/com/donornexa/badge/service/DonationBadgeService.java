package com.donornexa.badge.service;


import com.donornexa.badge.entity.DonationBadge;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.donation.repository.DonationRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing donor milestone gamification badges (Bronze Lifesaver, Silver Guardian, Gold Hero).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonationBadgeService {

    private final DonationRepository donationRepository;

    @Transactional(readOnly = true)
    public DonorBadgeProfile getBadgeProfileForDonor(Long donorId) {
        long completedCount = donationRepository.countByDonorIdAndDonationStatus(donorId, DonationStatus.COMPLETED);

        List<DonationBadge> earnedBadges = new ArrayList<>();
        DonationBadge highestBadge = null;
        DonationBadge nextBadge = DonationBadge.BRONZE_LIFESAVER;
        int nextTarget = 1;
        int progressPercent = 0;

        for (DonationBadge badge : DonationBadge.values()) {
            if (completedCount >= badge.getRequiredDonations()) {
                earnedBadges.add(badge);
                highestBadge = badge;
            }
        }

        if (highestBadge == null) {
            nextBadge = DonationBadge.BRONZE_LIFESAVER;
            nextTarget = 1;
            progressPercent = 0;
        } else if (highestBadge == DonationBadge.BRONZE_LIFESAVER) {
            nextBadge = DonationBadge.SILVER_GUARDIAN;
            nextTarget = 5;
            progressPercent = (int) Math.min(100, Math.round(((double) completedCount / 5.0) * 100));
        } else if (highestBadge == DonationBadge.SILVER_GUARDIAN) {
            nextBadge = DonationBadge.GOLD_HERO;
            nextTarget = 10;
            progressPercent = (int) Math.min(100, Math.round(((double) completedCount / 10.0) * 100));
        } else {
            nextBadge = null;
            nextTarget = 10;
            progressPercent = 100;
        }

        return DonorBadgeProfile.builder()
                .donorId(donorId)
                .totalCompletedDonations(completedCount)
                .highestBadge(highestBadge)
                .earnedBadges(earnedBadges)
                .nextBadge(nextBadge)
                .nextBadgeTarget(nextTarget)
                .progressToNextBadgePercent(progressPercent)
                .build();
    }

    @Getter
    @Builder
    public static class DonorBadgeProfile {
        private Long donorId;
        private long totalCompletedDonations;
        private DonationBadge highestBadge;
        private List<DonationBadge> earnedBadges;
        private DonationBadge nextBadge;
        private int nextBadgeTarget;
        private int progressToNextBadgePercent;
    }
}
