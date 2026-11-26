package com.donornexa.donation.repository;

import com.donornexa.common.enums.BloodGroup;


import com.donornexa.donation.entity.Donation;
import com.donornexa.common.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Donation} entities.
 */
@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    /** Find all donations made by a specific donor. */
    List<Donation> findByDonorId(Long donorId);

    /** Count completed donations made by a specific donor for milestone badges. */
    long countByDonorIdAndDonationStatus(Long donorId, DonationStatus status);

    /** Find all donations made by a specific donor ordered by date descending. */
    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    /** Find all donations made by a specific donor with eager-loaded hospital and bloodRequest. */
    @Query("SELECT d FROM Donation d JOIN FETCH d.hospital LEFT JOIN FETCH d.bloodRequest WHERE d.donor.id = :donorId ORDER BY d.donationDate DESC")
    List<Donation> findByDonorIdWithDetailsOrderByDonationDateDesc(@Param("donorId") Long donorId);

    /** Find all donations received by a specific hospital. */
    List<Donation> findByHospitalId(Long hospitalId);

    /** Find all donations with a specific status. */
    List<Donation> findByDonationStatus(DonationStatus status);

    /** Find all donations linked to a specific blood request. */
    List<Donation> findByBloodRequestId(Long bloodRequestId);

    /** Find donations for a donor with pagination. */
    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    /** Find donations for a hospital with pagination. */
    Page<Donation> findByHospitalId(Long hospitalId, Pageable pageable);

    /** Count donations for a hospital. */
    long countByHospitalId(Long hospitalId);

    /** Count donations for a hospital with a specific status. */
    long countByHospitalIdAndDonationStatus(Long hospitalId, DonationStatus status);

    /** Filter donations for hospital report with date-range and optional filters. */
    @Query(value = "SELECT d FROM Donation d " +
                   "JOIN FETCH d.donor dn " +
                   "JOIN FETCH dn.user dnu " +
                   "LEFT JOIN FETCH d.bloodRequest br " +
                   "WHERE d.hospital.id = :hospitalId " +
                   "  AND (:status IS NULL OR d.donationStatus = :status) " +
                   "  AND (:bloodGroup IS NULL OR dn.bloodGroup = :bloodGroup) " +
                   "  AND (:startDate IS NULL OR d.donationDate >= :startDate) " +
                   "  AND (:endDate IS NULL OR d.donationDate <= :endDate) " +
                   "ORDER BY d.donationDate DESC",
           countQuery = "SELECT COUNT(d) FROM Donation d " +
                        "JOIN d.donor dn " +
                        "WHERE d.hospital.id = :hospitalId " +
                        "  AND (:status IS NULL OR d.donationStatus = :status) " +
                        "  AND (:bloodGroup IS NULL OR dn.bloodGroup = :bloodGroup) " +
                        "  AND (:startDate IS NULL OR d.donationDate >= :startDate) " +
                        "  AND (:endDate IS NULL OR d.donationDate <= :endDate)")
    Page<Donation> filterHospitalDonations(
            @Param("hospitalId") Long hospitalId,
            @Param("status") DonationStatus status,
            @Param("bloodGroup") com.donornexa.common.enums.BloodGroup bloodGroup,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    /** Filter donations for admin report with date-range and optional filters. */
    @Query("SELECT d FROM Donation d " +
           "JOIN FETCH d.donor dn " +
           "JOIN FETCH dn.user dnu " +
           "JOIN FETCH d.hospital h " +
           "WHERE (:hospitalId IS NULL OR d.hospital.id = :hospitalId) " +
           "  AND (:status IS NULL OR d.donationStatus = :status) " +
           "  AND (:bloodGroup IS NULL OR dn.bloodGroup = :bloodGroup) " +
           "  AND (:startDate IS NULL OR d.donationDate >= :startDate) " +
           "  AND (:endDate IS NULL OR d.donationDate <= :endDate) " +
           "ORDER BY d.donationDate DESC")
    List<Donation> filterDonationsForReport(
            @Param("hospitalId") Long hospitalId,
            @Param("status") DonationStatus status,
            @Param("bloodGroup") com.donornexa.common.enums.BloodGroup bloodGroup,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    /** Find recent donations for a hospital. */
    @Query("SELECT d FROM Donation d JOIN FETCH d.donor dn JOIN FETCH dn.user dnu " +
           "LEFT JOIN FETCH d.bloodRequest br WHERE d.hospital.id = :hospitalId " +
           "ORDER BY d.donationDate DESC")
    List<Donation> findRecentByHospitalId(@Param("hospitalId") Long hospitalId, Pageable pageable);

    /** Search and filter donations for a hospital. */
    @Query(value = "SELECT d FROM Donation d " +
                   "JOIN FETCH d.donor dn " +
                   "JOIN FETCH dn.user dnu " +
                   "JOIN FETCH d.hospital h " +
                   "LEFT JOIN FETCH d.bloodRequest br " +
                   "WHERE d.hospital.id = :hospitalId " +
                   "  AND (:query IS NULL OR :query = '' " +
                   "       OR LOWER(dnu.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(dnu.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(dnu.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "  AND (:status IS NULL OR d.donationStatus = :status) " +
                   "ORDER BY d.donationDate DESC",
           countQuery = "SELECT COUNT(d) FROM Donation d " +
                        "JOIN d.donor dn " +
                        "JOIN dn.user dnu " +
                        "WHERE d.hospital.id = :hospitalId " +
                        "  AND (:query IS NULL OR :query = '' " +
                        "       OR LOWER(dnu.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(dnu.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(dnu.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "  AND (:status IS NULL OR d.donationStatus = :status)")
    Page<Donation> searchByHospitalAndStatus(
            @Param("hospitalId") Long hospitalId,
            @Param("query") String query,
            @Param("status") DonationStatus status,
            Pageable pageable);

    /** Count donations with a specific status. */
    long countByDonationStatus(DonationStatus status);

    /** Find donation by ID with eager-loaded donor, donor's user, hospital, and bloodRequest. */
    @Query("SELECT d FROM Donation d " +
           "JOIN FETCH d.donor dn " +
           "JOIN FETCH dn.user dnu " +
           "JOIN FETCH d.hospital h " +
           "LEFT JOIN FETCH d.bloodRequest br " +
           "WHERE d.id = :id")
    java.util.Optional<Donation> findByIdWithDetails(@Param("id") Long id);

    /**
     * Search and filter donations with pagination for the administrator.
     */
    @Query(value = "SELECT d FROM Donation d " +
                   "JOIN FETCH d.donor dn " +
                   "JOIN FETCH dn.user dnu " +
                   "JOIN FETCH d.hospital h " +
                   "LEFT JOIN FETCH d.bloodRequest br " +
                   "WHERE (:query IS NULL OR :query = '' " +
                   "       OR LOWER(dnu.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "  AND (:status IS NULL OR d.donationStatus = :status) " +
                   "ORDER BY d.donationDate DESC",
           countQuery = "SELECT COUNT(d) FROM Donation d " +
                        "JOIN d.donor dn " +
                        "JOIN dn.user dnu " +
                        "JOIN d.hospital h " +
                        "WHERE (:query IS NULL OR :query = '' " +
                        "       OR LOWER(dnu.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "  AND (:status IS NULL OR d.donationStatus = :status)")
    Page<Donation> searchAndFilter(
            @Param("query") String query,
            @Param("status") DonationStatus status,
            Pageable pageable);
}
