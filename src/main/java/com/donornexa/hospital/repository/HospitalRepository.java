package com.donornexa.hospital.repository;

import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;


import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Hospital} entities.
 */
@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    /** Find a hospital profile by the associated user ID. */
    Optional<Hospital> findByUserId(Long userId);

    /** Check whether a hospital with the given registration number exists. */
    boolean existsByRegistrationNumber(String registrationNumber);

    /** Check whether a hospital profile exists for the given user. */
    boolean existsByUserId(Long userId);

    /** Find all verified or unverified hospitals. */
    List<Hospital> findByVerified(Boolean verified);

    /** Find all hospitals in a specific province. */
    List<Hospital> findByProvinceId(Long provinceId);

    /** Find all hospitals in a specific city. */
    List<Hospital> findByCityId(Long cityId);

    /**
     * Eagerly fetch a hospital profile with its User, Province, and City by user email.
     */
    @Query("SELECT h FROM Hospital h " +
           "JOIN FETCH h.user u " +
           "JOIN FETCH h.province p " +
           "JOIN FETCH h.city c " +
           "WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Hospital> findByUserEmailWithDetails(@Param("email") String email);

    /**
     * Eagerly fetch a hospital profile with its User, Province, and City by hospital ID.
     */
    @Query("SELECT h FROM Hospital h " +
           "JOIN FETCH h.user u " +
           "JOIN FETCH h.province p " +
           "JOIN FETCH h.city c " +
           "WHERE h.id = :id")
    Optional<Hospital> findByIdWithDetails(@Param("id") Long id);

    /**
     * Search and filter hospitals with pagination for the administrator dashboard.
     */
    @Query(value = "SELECT h FROM Hospital h " +
                   "JOIN FETCH h.user u " +
                   "JOIN FETCH h.province p " +
                   "JOIN FETCH h.city c " +
                   "WHERE (:query IS NULL OR :query = '' " +
                   "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(h.registrationNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "  AND (:verificationStatus IS NULL OR :verificationStatus = '' " +
                   "       OR (:verificationStatus = 'VERIFIED' AND h.verified = true) " +
                   "       OR (:verificationStatus = 'REJECTED' AND h.verified = false AND h.rejectionReason IS NOT NULL AND h.rejectionReason <> '') " +
                   "       OR (:verificationStatus = 'PENDING' AND h.verified = false AND (h.rejectionReason IS NULL OR h.rejectionReason = ''))) " +
                   "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus) " +
                   "ORDER BY h.createdAt DESC",
           countQuery = "SELECT COUNT(h) FROM Hospital h " +
                        "JOIN h.user u " +
                        "WHERE (:query IS NULL OR :query = '' " +
                        "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(h.registrationNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "  AND (:verificationStatus IS NULL OR :verificationStatus = '' " +
                        "       OR (:verificationStatus = 'VERIFIED' AND h.verified = true) " +
                        "       OR (:verificationStatus = 'REJECTED' AND h.verified = false AND h.rejectionReason IS NOT NULL AND h.rejectionReason <> '') " +
                        "       OR (:verificationStatus = 'PENDING' AND h.verified = false AND (h.rejectionReason IS NULL OR h.rejectionReason = ''))) " +
                        "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)")
    Page<Hospital> searchAndFilter(
            @Param("query") String query,
            @Param("verificationStatus") String verificationStatus,
            @Param("accountStatus") AccountStatus accountStatus,
            Pageable pageable);

    /** Count hospitals by verification status. */
    long countByVerified(Boolean verified);

    /** Count pending verification hospitals (verified=false and no rejectionReason). */
    @Query("SELECT COUNT(h) FROM Hospital h WHERE h.verified = false AND (h.rejectionReason IS NULL OR h.rejectionReason = '')")
    long countPendingVerification();

    /** Count rejected hospitals (verified=false and rejectionReason is present). */
    @Query("SELECT COUNT(h) FROM Hospital h WHERE h.verified = false AND h.rejectionReason IS NOT NULL AND h.rejectionReason <> ''")
    long countRejectedVerification();

    /** Count hospitals by user account status. */
    long countByUserAccountStatus(AccountStatus accountStatus);
}
