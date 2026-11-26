package com.donornexa.donor.repository;


import com.donornexa.donor.entity.Donor;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Donor} entities.
 */
@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    /** Find a donor profile by the associated user ID. */
    Optional<Donor> findByUserId(Long userId);

    /** Find a donor profile with eager-loaded user, province, and city by user email. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user JOIN FETCH d.province JOIN FETCH d.city WHERE LOWER(d.user.email) = LOWER(:email)")
    Optional<Donor> findByUserEmailWithDetails(@Param("email") String email);

    @Query("SELECT d FROM Donor d JOIN FETCH d.user WHERE LOWER(d.user.email) = LOWER(:email)")
    Optional<Donor> findByUserEmail(@Param("email") String email);

    /** Find a donor profile with eager-loaded user, province, and city by donor ID. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user JOIN FETCH d.province JOIN FETCH d.city WHERE d.id = :id")
    Optional<Donor> findByIdWithDetails(@Param("id") Long id);

    /** Find a donor profile with eager-loaded user, province, and city by user ID. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user JOIN FETCH d.province JOIN FETCH d.city WHERE d.user.id = :userId")
    Optional<Donor> findByUserIdWithDetails(@Param("userId") Long userId);

    /** Check whether a donor profile exists for the given user. */
    boolean existsByUserId(Long userId);

    /** Find all donors with the specified blood group. */
    List<Donor> findByBloodGroup(BloodGroup bloodGroup);

    /** Count donors with the specified blood group. */
    long countByBloodGroup(BloodGroup bloodGroup);

    /** Find all donors with the specified availability status. */
    List<Donor> findByAvailability(DonorAvailability availability);

    /** Count donors with the specified availability status. */
    long countByAvailability(DonorAvailability availability);

    /** Find all donors in a specific city. */
    List<Donor> findByCityId(Long cityId);

    /** Find all donors matching any of the specified blood groups with eager user, province, and city. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user u JOIN FETCH d.province p JOIN FETCH d.city c WHERE d.bloodGroup IN :bloodGroups")
    List<Donor> findByBloodGroupInWithDetails(@Param("bloodGroups") java.util.Collection<BloodGroup> bloodGroups);

    /** Find all donors with eager user, province, and city. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user u JOIN FETCH d.province p JOIN FETCH d.city c")
    List<Donor> findAllWithDetails();

    /** Find all donors in a specific province. */
    List<Donor> findByProvinceId(Long provinceId);

    /** Find donors by blood group and availability, with pagination support. */
    Page<Donor> findByBloodGroupAndAvailability(BloodGroup bloodGroup,
                                                 DonorAvailability availability,
                                                 Pageable pageable);

    /** Find verified or unverified donors. */
    List<Donor> findByVerified(Boolean verified);

    /** Find all verified donors with user eager loaded. */
    @Query("SELECT d FROM Donor d JOIN FETCH d.user u WHERE d.verified = true ORDER BY u.fullName ASC")
    List<Donor> findAllVerifiedWithUser();

    /** Count donors by verified status. */
    long countByVerified(Boolean verified);

    /** Find donors in a city with a specific blood group. */
    List<Donor> findByCityIdAndBloodGroup(Long cityId, BloodGroup bloodGroup);

    /** Find donors in a province with a specific blood group. */
    List<Donor> findByProvinceIdAndBloodGroup(Long provinceId, BloodGroup bloodGroup);

    /**
     * Search and filter donors with pagination for the administrator dashboard.
     */
    @Query(value = "SELECT d FROM Donor d " +
                   "JOIN FETCH d.user u " +
                   "JOIN FETCH d.province p " +
                   "JOIN FETCH d.city c " +
                   "WHERE (:query IS NULL OR :query = '' " +
                   "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "  AND (:verificationStatus IS NULL OR :verificationStatus = '' " +
                   "       OR (:verificationStatus = 'VERIFIED' AND d.verified = true) " +
                   "       OR (:verificationStatus = 'REJECTED' AND d.verified = false AND d.rejectionReason IS NOT NULL AND d.rejectionReason <> '') " +
                   "       OR (:verificationStatus = 'PENDING' AND d.verified = false AND (d.rejectionReason IS NULL OR d.rejectionReason = ''))) " +
                   "  AND (:bloodGroup IS NULL OR d.bloodGroup = :bloodGroup) " +
                   "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus) " +
                   "ORDER BY d.createdAt DESC",
           countQuery = "SELECT COUNT(d) FROM Donor d " +
                        "JOIN d.user u " +
                        "WHERE (:query IS NULL OR :query = '' " +
                        "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "  AND (:verificationStatus IS NULL OR :verificationStatus = '' " +
                        "       OR (:verificationStatus = 'VERIFIED' AND d.verified = true) " +
                        "       OR (:verificationStatus = 'REJECTED' AND d.verified = false AND d.rejectionReason IS NOT NULL AND d.rejectionReason <> '') " +
                        "       OR (:verificationStatus = 'PENDING' AND d.verified = false AND (d.rejectionReason IS NULL OR d.rejectionReason = ''))) " +
                        "  AND (:bloodGroup IS NULL OR d.bloodGroup = :bloodGroup) " +
                        "  AND (:accountStatus IS NULL OR u.accountStatus = :accountStatus)")
    Page<Donor> searchAndFilter(
            @Param("query") String query,
            @Param("verificationStatus") String verificationStatus,
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("accountStatus") AccountStatus accountStatus,
            Pageable pageable);

    /** Count pending verification donors (verified=false and no rejectionReason). */
    @Query("SELECT COUNT(d) FROM Donor d WHERE d.verified = false AND (d.rejectionReason IS NULL OR d.rejectionReason = '')")
    long countPendingVerification();

    /** Count rejected donors (verified=false and rejectionReason is present). */
    @Query("SELECT COUNT(d) FROM Donor d WHERE d.verified = false AND d.rejectionReason IS NOT NULL AND d.rejectionReason <> ''")
    long countRejectedVerification();

    /** Count donors by user account status. */
    long countByUserAccountStatus(AccountStatus accountStatus);
}
