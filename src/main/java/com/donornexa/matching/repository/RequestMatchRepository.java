package com.donornexa.matching.repository;


import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RequestMatch} entities.
 */
@Repository
public interface RequestMatchRepository extends JpaRepository<RequestMatch, Long> {

    /** Find all matches for a specific donor. */
    List<RequestMatch> findByDonorId(Long donorId);

    /** Find all matches for a specific blood request. */
    List<RequestMatch> findByBloodRequestId(Long bloodRequestId);

    /**
     * Check whether a match already exists between the given donor and blood request.
     * Used to enforce the composite unique constraint at the application level.
     */
    boolean existsByDonorIdAndBloodRequestId(Long donorId, Long bloodRequestId);

    /** Find specific match by donor ID and blood request ID. */
    Optional<RequestMatch> findByDonorIdAndBloodRequestId(Long donorId, Long bloodRequestId);

    /** Find all matches for a donor with a specific status. */
    List<RequestMatch> findByDonorIdAndMatchStatus(Long donorId, MatchStatus matchStatus);

    /** Find all matches for a blood request with a specific status. */
    List<RequestMatch> findByBloodRequestIdAndMatchStatus(Long bloodRequestId, MatchStatus matchStatus);

    /** Find match by ID with eager-loaded donor, user, blood request, and hospital details. */
    @Query("SELECT rm FROM RequestMatch rm " +
           "JOIN FETCH rm.bloodRequest br " +
           "JOIN FETCH br.hospital h " +
           "JOIN FETCH br.province bp " +
           "JOIN FETCH br.city bc " +
           "JOIN FETCH rm.donor d " +
           "JOIN FETCH d.user u " +
           "JOIN FETCH d.province dp " +
           "JOIN FETCH d.city dc " +
           "WHERE rm.id = :id")
    Optional<RequestMatch> findByIdWithDetails(@Param("id") Long id);

    /** Find all matches for a blood request ordered by score descending. */
    @Query("SELECT rm FROM RequestMatch rm " +
           "JOIN FETCH rm.donor d " +
           "JOIN FETCH d.user u " +
           "JOIN FETCH d.province dp " +
           "JOIN FETCH d.city dc " +
           "JOIN FETCH rm.bloodRequest br " +
           "JOIN FETCH br.hospital h " +
           "WHERE br.id = :bloodRequestId " +
           "ORDER BY rm.matchScore DESC, rm.matchedAt DESC")
    List<RequestMatch> findByBloodRequestIdWithDetailsOrderByScoreDesc(@Param("bloodRequestId") Long bloodRequestId);

    /** Find all matches for a donor by user email ordered newest first. */
    @Query("SELECT rm FROM RequestMatch rm " +
           "JOIN FETCH rm.bloodRequest br " +
           "JOIN FETCH br.hospital h " +
           "JOIN FETCH br.province bp " +
           "JOIN FETCH br.city bc " +
           "JOIN FETCH rm.donor d " +
           "JOIN FETCH d.user u " +
           "WHERE LOWER(u.email) = LOWER(:email) " +
           "ORDER BY rm.matchedAt DESC")
    List<RequestMatch> findByDonorUserEmailWithDetails(@Param("email") String email);

    /** Find paginated matches for a donor by user email. */
    @Query(value = "SELECT rm FROM RequestMatch rm " +
                   "JOIN FETCH rm.bloodRequest br " +
                   "JOIN FETCH br.hospital h " +
                   "JOIN FETCH br.province bp " +
                   "JOIN FETCH br.city bc " +
                   "JOIN FETCH rm.donor d " +
                   "JOIN FETCH d.user u " +
                   "WHERE LOWER(u.email) = LOWER(:email) " +
                   "ORDER BY rm.matchedAt DESC",
           countQuery = "SELECT COUNT(rm) FROM RequestMatch rm " +
                        "JOIN rm.donor d JOIN d.user u " +
                        "WHERE LOWER(u.email) = LOWER(:email)")
    Page<RequestMatch> findByDonorUserEmailWithDetails(@Param("email") String email, Pageable pageable);

    /** Count matches by status. */
    long countByMatchStatus(MatchStatus matchStatus);

    /** Count matches for a hospital. */
    long countByBloodRequestHospitalId(Long hospitalId);

    /** Count matches for a hospital by status. */
    long countByBloodRequestHospitalIdAndMatchStatus(Long hospitalId, MatchStatus matchStatus);

    @Query("SELECT rm FROM RequestMatch rm " +
           "JOIN FETCH rm.donor d " +
           "JOIN FETCH d.user u " +
           "JOIN FETCH rm.bloodRequest br " +
           "WHERE br.hospital.id = :hospitalId AND rm.matchStatus IN :statuses " +
           "ORDER BY rm.matchedAt DESC")
    List<RequestMatch> findByHospitalIdAndMatchStatusInWithDetails(
            @Param("hospitalId") Long hospitalId,
            @Param("statuses") java.util.Collection<MatchStatus> statuses);

    @Query("SELECT rm FROM RequestMatch rm " +
           "JOIN FETCH rm.bloodRequest br " +
           "JOIN FETCH br.hospital h " +
           "JOIN FETCH br.city bc " +
           "JOIN FETCH rm.donor d " +
           "JOIN FETCH d.user u " +
           "WHERE LOWER(u.email) = LOWER(:email) AND rm.matchStatus IN :statuses " +
           "ORDER BY rm.matchedAt DESC")
    List<RequestMatch> findByDonorEmailAndMatchStatusInWithDetails(
            @Param("email") String email,
            @Param("statuses") java.util.Collection<MatchStatus> statuses);

    /** Count matches for a donor user by email and match status. */
    @Query("SELECT COUNT(rm) FROM RequestMatch rm JOIN rm.donor d JOIN d.user u WHERE LOWER(u.email) = LOWER(:email) AND rm.matchStatus = :status")
    long countByDonorUserEmailAndMatchStatus(@Param("email") String email, @Param("status") MatchStatus status);

    /** Count matches for a donor user by email and statuses collection. */
    @Query("SELECT COUNT(rm) FROM RequestMatch rm JOIN rm.donor d JOIN d.user u WHERE LOWER(u.email) = LOWER(:email) AND rm.matchStatus IN :statuses")
    long countByDonorUserEmailAndMatchStatusIn(@Param("email") String email, @Param("statuses") java.util.Collection<MatchStatus> statuses);

    /** Filter matches for hospital report. */
    @Query(value = "SELECT rm FROM RequestMatch rm " +
                   "JOIN FETCH rm.bloodRequest br " +
                   "JOIN FETCH rm.donor d " +
                   "JOIN FETCH d.user u " +
                   "WHERE br.hospital.id = :hospitalId " +
                   "  AND (:status IS NULL OR rm.matchStatus = :status) " +
                   "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                   "  AND (:startDate IS NULL OR rm.matchedAt >= :startDate) " +
                   "  AND (:endDate IS NULL OR rm.matchedAt <= :endDate) " +
                   "ORDER BY rm.matchedAt DESC",
           countQuery = "SELECT COUNT(rm) FROM RequestMatch rm " +
                        "JOIN rm.bloodRequest br " +
                        "WHERE br.hospital.id = :hospitalId " +
                        "  AND (:status IS NULL OR rm.matchStatus = :status) " +
                        "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                        "  AND (:startDate IS NULL OR rm.matchedAt >= :startDate) " +
                        "  AND (:endDate IS NULL OR rm.matchedAt <= :endDate)")
    Page<RequestMatch> filterHospitalMatches(
            @Param("hospitalId") Long hospitalId,
            @Param("status") MatchStatus status,
            @Param("bloodGroup") BloodGroup bloodGroup,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    /** Count matches for a specific blood request. */
    long countByBloodRequestId(Long bloodRequestId);

    /**
     * Search and filter matches with pagination for administrative monitoring.
     */
    @Query(value = "SELECT rm FROM RequestMatch rm " +
                   "JOIN FETCH rm.bloodRequest br " +
                   "JOIN FETCH br.hospital h " +
                   "JOIN FETCH rm.donor d " +
                   "JOIN FETCH d.user u " +
                   "WHERE (:query IS NULL OR :query = '' " +
                   "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                   "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                   "  AND (:status IS NULL OR rm.matchStatus = :status) " +
                   "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                   "ORDER BY rm.matchedAt DESC",
           countQuery = "SELECT COUNT(rm) FROM RequestMatch rm " +
                        "JOIN rm.bloodRequest br JOIN br.hospital h " +
                        "JOIN rm.donor d JOIN d.user u " +
                        "WHERE (:query IS NULL OR :query = '' " +
                        "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "       OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "  AND (:status IS NULL OR rm.matchStatus = :status) " +
                        "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup)")
    Page<RequestMatch> searchAndFilter(@Param("query") String query,
                                       @Param("status") MatchStatus status,
                                       @Param("bloodGroup") BloodGroup bloodGroup,
                                       Pageable pageable);
}
