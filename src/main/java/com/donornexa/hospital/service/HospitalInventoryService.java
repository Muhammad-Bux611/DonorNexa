package com.donornexa.hospital.service;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.StockEntryType;
import com.donornexa.common.enums.StockItemStatus;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.inventory.entity.BloodInventory;
import com.donornexa.inventory.entity.BloodStockEntry;

import com.donornexa.hospital.dto.DirectStockEntryDTO;
import com.donornexa.hospital.dto.HospitalInventorySummaryDTO;
import com.donornexa.hospital.dto.IssueStockDTO;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.inventory.repository.BloodInventoryRepository;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.inventory.repository.BloodStockEntryRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalInventoryService {

    private final BloodInventoryRepository bloodInventoryRepository;
    private final BloodStockEntryRepository bloodStockEntryRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodRequestRepository bloodRequestRepository;

    @Transactional(readOnly = true)
    public Hospital getHospitalByEmail(String email) {
        return hospitalRepository.findByUserEmailWithDetails(email.toLowerCase().trim())
                .orElseThrow(() -> new HospitalNotFoundException("Hospital facility not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public HospitalInventorySummaryDTO getInventorySummary(String email) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryThreshold = today.plusDays(7);

        long totalAvailable = bloodStockEntryRepository.sumTotalAvailableUnitsByHospital(hospital.getId(), today);
        long expiredUnits = bloodStockEntryRepository.sumExpiredUnitsByHospital(hospital.getId(), today);
        long nearExpiryUnits = bloodStockEntryRepository.sumNearExpiryUnitsByHospital(hospital.getId(), today, nearExpiryThreshold);

        // Calculate reserved units from aggregate inventory
        List<BloodInventory> aggList = bloodInventoryRepository.findByHospitalId(hospital.getId());
        long totalReserved = aggList.stream().mapToLong(b -> b.getReservedUnits() != null ? b.getReservedUnits() : 0).sum();

        // Blood group breakdown across all 8 standard blood groups
        Map<BloodGroup, Long> unitsByGroup = new LinkedHashMap<>();
        for (BloodGroup bg : BloodGroup.values()) {
            long count = bloodStockEntryRepository.sumAvailableUnitsByHospitalAndGroup(hospital.getId(), bg, today);
            unitsByGroup.put(bg, count);
        }

        List<BloodStockEntry> activeBatches = bloodStockEntryRepository
                .findByHospitalIdAndStatusOrderByExpiryDateAsc(hospital.getId(), StockItemStatus.AVAILABLE);

        List<BloodStockEntry> nearExpiryBatches = activeBatches.stream()
                .filter(b -> b.isNearExpiry(7))
                .toList();

        List<BloodStockEntry> expiredBatches = bloodStockEntryRepository
                .findByHospitalIdAndStatusAndExpiryDateLessThan(hospital.getId(), StockItemStatus.AVAILABLE, today);

        return HospitalInventorySummaryDTO.builder()
                .totalAvailableUnits(totalAvailable)
                .totalReservedUnits(totalReserved)
                .nearExpiryUnits(nearExpiryUnits)
                .expiredUnits(expiredUnits)
                .unitsByGroup(unitsByGroup)
                .activeBatches(activeBatches)
                .nearExpiryBatches(nearExpiryBatches)
                .expiredBatches(expiredBatches)
                .build();
    }

    @Transactional
    public BloodStockEntry addStockFromDonation(Hospital hospital, Donation donation) {
        if (donation.getDonationStatus() != DonationStatus.COMPLETED) {
            throw new HospitalOperationException("Clinical protocol violation: Stock can only be entered from COMPLETED donations.");
        }

        if (bloodStockEntryRepository.existsByDonationId(donation.getId())) {
            throw new HospitalOperationException("Duplicate entry prevention: Inventory stock has already been logged for donation ID #" + donation.getId());
        }

        BloodGroup bloodGroup = donation.getDonor().getBloodGroup();
        Integer units = donation.getUnitsDonated();
        LocalDate collectionDate = donation.getDonationDate().toLocalDate();
        LocalDate expiryDate = collectionDate.plusDays(35); // standard whole blood storage

        BloodStockEntry entry = new BloodStockEntry();
        entry.setHospital(hospital);
        entry.setBloodGroup(bloodGroup);
        entry.setUnits(units);
        entry.setCollectionDate(collectionDate);
        entry.setExpiryDate(expiryDate);
        entry.setEntryType(StockEntryType.DONATION_INTAKE);
        entry.setStatus(StockItemStatus.AVAILABLE);
        entry.setDonation(donation);
        entry.setBloodRequest(donation.getBloodRequest());
        entry.setNotes("Intake from voluntary donation #" + donation.getId() + " (Donor: " + donation.getDonor().getUser().getFullName() + ")");

        BloodStockEntry savedEntry = bloodStockEntryRepository.save(entry);
        syncAggregateInventory(hospital, bloodGroup);

        log.info("Logged {} unit(s) of {} into inventory from donation #{} for hospital {}",
                units, bloodGroup, donation.getId(), hospital.getId());
        return savedEntry;
    }

    @Transactional
    public BloodStockEntry addDirectStock(String email, DirectStockEntryDTO form) {
        Hospital hospital = getHospitalByEmail(email);

        if (form.getUnits() == null || form.getUnits() < 1) {
            throw new HospitalOperationException("Quantity must be at least 1 unit.");
        }

        LocalDate collectionDate = form.getCollectionDate() != null ? form.getCollectionDate() : LocalDate.now();
        if (collectionDate.isAfter(LocalDate.now())) {
            throw new HospitalOperationException("Collection date cannot be in the future.");
        }

        int shelfLife = form.getShelfLifeDays() != null ? form.getShelfLifeDays() : 35;
        LocalDate expiryDate = collectionDate.plusDays(shelfLife);

        BloodStockEntry entry = new BloodStockEntry();
        entry.setHospital(hospital);
        entry.setBloodGroup(form.getBloodGroup());
        entry.setUnits(form.getUnits());
        entry.setCollectionDate(collectionDate);
        entry.setExpiryDate(expiryDate);
        entry.setEntryType(StockEntryType.DIRECT_INTAKE);
        entry.setStatus(StockItemStatus.AVAILABLE);
        entry.setNotes(form.getNotes() != null && !form.getNotes().isBlank() ? form.getNotes().trim() : "Direct blood bank intake batch");

        BloodStockEntry savedEntry = bloodStockEntryRepository.save(entry);
        syncAggregateInventory(hospital, form.getBloodGroup());

        log.info("Logged direct intake of {} units of {} for hospital {}",
                form.getUnits(), form.getBloodGroup(), hospital.getId());
        return savedEntry;
    }

    @Transactional
    public void issueStock(String email, IssueStockDTO form) {
        Hospital hospital = getHospitalByEmail(email);
        BloodGroup bloodGroup = form.getBloodGroup();
        int unitsToIssue = form.getUnits();
        LocalDate today = LocalDate.now();

        if (unitsToIssue < 1) {
            throw new HospitalOperationException("Units to issue must be at least 1.");
        }

        long availableUnits = bloodStockEntryRepository.sumAvailableUnitsByHospitalAndGroup(hospital.getId(), bloodGroup, today);
        if (availableUnits < unitsToIssue) {
            throw new HospitalOperationException(String.format(
                    "Insufficient unexpired stock for blood group %s. Available: %d unit(s), Requested: %d unit(s).",
                    bloodGroup.name().replace('_', ' '), availableUnits, unitsToIssue));
        }

        BloodRequest bloodRequest = null;
        if (form.getBloodRequestId() != null) {
            bloodRequest = bloodRequestRepository.findById(form.getBloodRequestId())
                    .orElseThrow(() -> new HospitalOperationException("Linked blood request not found."));
            if (!bloodRequest.getHospital().getId().equals(hospital.getId())) {
                throw new HospitalOperationException("Access Denied: You cannot issue blood against another hospital's request.");
            }
        }

        // FIFO deduction: take batches with earliest expiry date
        List<BloodStockEntry> availableBatches = bloodStockEntryRepository
                .findByHospitalIdAndBloodGroupAndStatusOrderByExpiryDateAsc(hospital.getId(), bloodGroup, StockItemStatus.AVAILABLE)
                .stream()
                .filter(b -> !b.isExpired())
                .toList();

        int remaining = unitsToIssue;
        for (BloodStockEntry batch : availableBatches) {
            if (remaining <= 0) break;

            if (batch.getUnits() <= remaining) {
                // Entire batch consumed
                remaining -= batch.getUnits();
                batch.setStatus(StockItemStatus.ISSUED);
                if (bloodRequest != null) batch.setBloodRequest(bloodRequest);
                if (form.getNotes() != null) batch.setNotes(batch.getNotes() + " | Issued: " + form.getNotes());
                bloodStockEntryRepository.save(batch);
            } else {
                // Batch partially consumed: reduce existing batch and log an issued split batch
                int consumedFromBatch = remaining;
                batch.setUnits(batch.getUnits() - consumedFromBatch);
                bloodStockEntryRepository.save(batch);

                BloodStockEntry issuedPortion = new BloodStockEntry();
                issuedPortion.setHospital(hospital);
                issuedPortion.setBloodGroup(bloodGroup);
                issuedPortion.setUnits(consumedFromBatch);
                issuedPortion.setCollectionDate(batch.getCollectionDate());
                issuedPortion.setExpiryDate(batch.getExpiryDate());
                issuedPortion.setEntryType(StockEntryType.REQUEST_DISPATCH);
                issuedPortion.setStatus(StockItemStatus.ISSUED);
                issuedPortion.setBloodRequest(bloodRequest);
                issuedPortion.setDonation(batch.getDonation());
                issuedPortion.setNotes("Issued from batch #" + batch.getId() + (form.getNotes() != null ? " - " + form.getNotes() : ""));
                bloodStockEntryRepository.save(issuedPortion);

                remaining = 0;
            }
        }

        // If linked to blood request, update request fulfillment count
        if (bloodRequest != null) {
            int fulfilled = (bloodRequest.getUnitsFulfilled() != null ? bloodRequest.getUnitsFulfilled() : 0) + unitsToIssue;
            bloodRequest.setUnitsFulfilled(fulfilled);
            if (fulfilled >= bloodRequest.getUnitsRequired()) {
                bloodRequest.setStatus(RequestStatus.FULFILLED);
            }
            bloodRequestRepository.save(bloodRequest);
        }

        // Synchronize aggregate inventory
        syncAggregateInventory(hospital, bloodGroup);

        log.info("Issued {} unit(s) of {} by hospital {} (linked request: {})",
                unitsToIssue, bloodGroup, hospital.getId(), form.getBloodRequestId());
    }

    @Transactional
    public int discardExpiredStock(String email) {
        Hospital hospital = getHospitalByEmail(email);
        LocalDate today = LocalDate.now();

        List<BloodStockEntry> expiredStock = bloodStockEntryRepository
                .findByHospitalIdAndStatusAndExpiryDateLessThan(hospital.getId(), StockItemStatus.AVAILABLE, today);

        int count = 0;
        Set<BloodGroup> affectedGroups = new HashSet<>();
        for (BloodStockEntry batch : expiredStock) {
            batch.setStatus(StockItemStatus.DISCARDED);
            batch.setNotes((batch.getNotes() != null ? batch.getNotes() + " | " : "") + "Discarded expired stock on " + today);
            bloodStockEntryRepository.save(batch);
            affectedGroups.add(batch.getBloodGroup());
            count += batch.getUnits();
        }

        for (BloodGroup bg : affectedGroups) {
            syncAggregateInventory(hospital, bg);
        }

        log.info("Discarded {} expired unit(s) across {} groups for hospital {}",
                count, affectedGroups.size(), hospital.getId());
        return count;
    }

    @Transactional(readOnly = true)
    public Page<BloodStockEntry> getStockHistory(String email, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(email);
        return bloodStockEntryRepository.findHistoryByHospitalId(hospital.getId(), pageable);
    }

    /**
     * Re-synchronizes the aggregate {@link BloodInventory} table for a given hospital & blood group
     * based on active non-expired units in {@link BloodStockEntry}.
     */
    @Transactional
    public void syncAggregateInventory(Hospital hospital, BloodGroup bloodGroup) {
        LocalDate today = LocalDate.now();
        long availableCount = bloodStockEntryRepository.sumAvailableUnitsByHospitalAndGroup(hospital.getId(), bloodGroup, today);

        BloodInventory aggregate = bloodInventoryRepository
                .findByHospitalIdAndBloodGroup(hospital.getId(), bloodGroup)
                .orElseGet(() -> {
                    BloodInventory inv = new BloodInventory();
                    inv.setHospital(hospital);
                    inv.setBloodGroup(bloodGroup);
                    inv.setReservedUnits(0);
                    return inv;
                });

        aggregate.setAvailableUnits((int) Math.max(0, availableCount));
        aggregate.setLastUpdated(LocalDateTime.now());
        aggregate.recalculateStatus();

        // Set nearest expiry date among active batches
        List<BloodStockEntry> active = bloodStockEntryRepository
                .findByHospitalIdAndBloodGroupAndStatusOrderByExpiryDateAsc(hospital.getId(), bloodGroup, StockItemStatus.AVAILABLE);
        if (!active.isEmpty()) {
            aggregate.setExpiryDate(active.get(0).getExpiryDate().atStartOfDay());
            aggregate.setCollectionDate(active.get(active.size() - 1).getCollectionDate().atStartOfDay());
        }

        bloodInventoryRepository.save(aggregate);
    }
}
