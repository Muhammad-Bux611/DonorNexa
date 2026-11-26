package com.donornexa.inventory.repository;


import com.donornexa.inventory.entity.BloodStockEntry;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.StockItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodStockEntryRepository extends JpaRepository<BloodStockEntry, Long> {

    List<BloodStockEntry> findByHospitalIdOrderByCollectionDateDesc(Long hospitalId);

    List<BloodStockEntry> findByHospitalIdAndStatusOrderByExpiryDateAsc(Long hospitalId, StockItemStatus status);

    List<BloodStockEntry> findByHospitalIdAndBloodGroupAndStatusOrderByExpiryDateAsc(
            Long hospitalId, BloodGroup bloodGroup, StockItemStatus status);

    List<BloodStockEntry> findByHospitalIdAndStatusAndExpiryDateLessThan(
            Long hospitalId, StockItemStatus status, LocalDate date);

    List<BloodStockEntry> findByHospitalIdAndStatusAndExpiryDateBetween(
            Long hospitalId, StockItemStatus status, LocalDate start, LocalDate end);

    boolean existsByDonationId(Long donationId);

    Optional<BloodStockEntry> findByDonationId(Long donationId);

    @Query("SELECT COALESCE(SUM(b.units), 0) FROM BloodStockEntry b " +
           "WHERE b.hospital.id = :hospitalId AND b.bloodGroup = :bloodGroup " +
           "AND b.status = 'AVAILABLE' AND b.expiryDate >= :today")
    long sumAvailableUnitsByHospitalAndGroup(
            @Param("hospitalId") Long hospitalId,
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.units), 0) FROM BloodStockEntry b " +
           "WHERE b.hospital.id = :hospitalId AND b.status = 'AVAILABLE' AND b.expiryDate >= :today")
    long sumTotalAvailableUnitsByHospital(
            @Param("hospitalId") Long hospitalId,
            @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.units), 0) FROM BloodStockEntry b " +
           "WHERE b.hospital.id = :hospitalId AND b.status = 'AVAILABLE' AND b.expiryDate < :today")
    long sumExpiredUnitsByHospital(
            @Param("hospitalId") Long hospitalId,
            @Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(b.units), 0) FROM BloodStockEntry b " +
           "WHERE b.hospital.id = :hospitalId AND b.status = 'AVAILABLE' " +
           "AND b.expiryDate BETWEEN :today AND :threshold")
    long sumNearExpiryUnitsByHospital(
            @Param("hospitalId") Long hospitalId,
            @Param("today") LocalDate today,
            @Param("threshold") LocalDate threshold);

    @Query(value = "SELECT b FROM BloodStockEntry b " +
                   "LEFT JOIN FETCH b.donation d " +
                   "LEFT JOIN FETCH b.bloodRequest br " +
                   "WHERE b.hospital.id = :hospitalId ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM BloodStockEntry b WHERE b.hospital.id = :hospitalId")
    Page<BloodStockEntry> findHistoryByHospitalId(
            @Param("hospitalId") Long hospitalId,
            Pageable pageable);

    @Query("SELECT b FROM BloodStockEntry b " +
           "LEFT JOIN FETCH b.hospital h " +
           "LEFT JOIN FETCH b.donation d " +
           "WHERE b.status = 'AVAILABLE' AND b.expiryDate < :today")
    List<BloodStockEntry> findAllExpiredActiveStock(@Param("today") LocalDate today);
}
