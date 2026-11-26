package com.donornexa.admin.service;

import com.donornexa.donation.entity.Donation;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.donation.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Service managing administrative oversight of donation records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDonationService {

    private final DonationRepository donationRepository;

    @Transactional(readOnly = true)
    public Page<Donation> searchDonations(String query, DonationStatus status, Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return donationRepository.searchAndFilter(cleanQuery, status, pageable);
    }

    @Transactional(readOnly = true)
    public Donation getDonationById(Long id) {
        return donationRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NoSuchElementException("Donation record not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public long getCompletedDonationsCount() {
        return donationRepository.countByDonationStatus(DonationStatus.COMPLETED);
    }
}
