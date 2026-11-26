package com.donornexa.bloodrequest.repository;


import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link BloodRequest} entities.
 */
@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    /** Find all blood requests with the given status. */
    List<BloodRequest> findByStatus(RequestStatus status);

    /** Find all blood requests for a specific blood group. */
    List<BloodRequest> findByBloodGroup(BloodGroup bloodGroup);

    /** Find all blood requests raised by a specific hospital. */
    List<BloodRequest> findByHospitalId(Long hospitalId);

    /** Find all blood requests in a specific city. */
    List<BloodRequest> findByCityId(Long cityId);

    /** Find all blood requests in a specific province. */
    List<BloodRequest> findByProvinceId(Long provinceId);

    /** Find blood requests by urgency level. */
    List<BloodRequest> findByUrgency(RequestUrgency urgency);

    /** Find blood requests by blood group and status, with pagination. */
    Page<BloodRequest> findByBloodGroupAndStatus(BloodGroup bloodGroup,
                                                  RequestStatus status,
                                                  Pageable pageable);

    /** Find open requests for a specific blood group in a city. */
    List<BloodRequest> findByCityIdAndBloodGroupAndStatus(Long cityId,
                                                           BloodGroup bloodGroup,
                                                           RequestStatus status);

    /** Find open requests for a specific blood group in a province. */
    List<BloodRequest> findByProvinceIdAndBloodGroupAndStatus(Long provinceId,
                                                               BloodGroup bloodGroup,
                                                               RequestStatus status);

    /** Find all blood requests for a hospital with pagination. */
    Page<BloodRequest> findByHospitalId(Long hospitalId, Pageable pageable);

    /** Count all blood requests for a hospital. */
    long countByHospitalId(Long hospitalId);

    /** Count blood requests for a hospital by status. */
    long countByHospitalIdAndStatus(Long hospitalId, RequestStatus status);

    /** Count blood requests for a hospital with statuses in collection. */
    long countByHospitalIdAndStatusIn(Long hospitalId, java.util.Collection<RequestStatus> statuses);

    /** Find top 5 recent blood requests across platform. */
    @org.springframework.data.jpa.repository.Query("SELECT br FROM BloodRequest br JOIN FETCH br.hospital h JOIN FETCH br.city c ORDER BY br.createdAt DESC")
    List<BloodRequest> findTop5RecentWithDetails(Pageable pageable);

    /** Find top 5 recent blood requests for a hospital. */
    List<BloodRequest> findTop5ByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    /** Filter blood requests for hospital reports with optional filters. */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT br FROM BloodRequest br " +
                    "JOIN FETCH br.hospital h " +
                    "JOIN FETCH br.city c " +
                    "WHERE br.hospital.id = :hospitalId " +
                    "  AND (:status IS NULL OR br.status = :status) " +
                    "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                    "  AND (:startDate IS NULL OR br.createdAt >= :startDate) " +
                    "  AND (:endDate IS NULL OR br.createdAt <= :endDate) " +
                    "ORDER BY br.createdAt DESC",
            countQuery = "SELECT COUNT(br) FROM BloodRequest br " +
                         "WHERE br.hospital.id = :hospitalId " +
                         "  AND (:status IS NULL OR br.status = :status) " +
                         "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                         "  AND (:startDate IS NULL OR br.createdAt >= :startDate) " +
                         "  AND (:endDate IS NULL OR br.createdAt <= :endDate)")
    Page<BloodRequest> filterHospitalRequests(
            @org.springframework.data.repository.query.Param("hospitalId") Long hospitalId,
            @org.springframework.data.repository.query.Param("status") RequestStatus status,
            @org.springframework.data.repository.query.Param("bloodGroup") BloodGroup bloodGroup,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);

    /** Filter all blood requests for admin reports. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT br FROM BloodRequest br " +
            "JOIN FETCH br.hospital h " +
            "JOIN FETCH br.city c " +
            "WHERE (:hospitalId IS NULL OR br.hospital.id = :hospitalId) " +
            "  AND (:status IS NULL OR br.status = :status) " +
            "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
            "  AND (:startDate IS NULL OR br.createdAt >= :startDate) " +
            "  AND (:endDate IS NULL OR br.createdAt <= :endDate) " +
            "ORDER BY br.createdAt DESC")
    List<BloodRequest> filterRequestsForReport(
            @org.springframework.data.repository.query.Param("hospitalId") Long hospitalId,
            @org.springframework.data.repository.query.Param("status") RequestStatus status,
            @org.springframework.data.repository.query.Param("bloodGroup") BloodGroup bloodGroup,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDateTime endDate);

    /** Count blood requests by status. */
    long countByStatus(RequestStatus status);

    /** Count blood requests with statuses in the provided collection. */
    long countByStatusIn(java.util.Collection<RequestStatus> statuses);

    long countByHospitalIdAndUrgencyAndStatusIn(Long hospitalId, RequestUrgency urgency, java.util.Collection<RequestStatus> statuses);

    List<BloodRequest> findByStatusInAndRequiredByBefore(java.util.Collection<RequestStatus> statuses, java.time.LocalDateTime dateTime);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT br FROM BloodRequest br WHERE br.id = :id")
    java.util.Optional<BloodRequest> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(br.unitsRequired), 0) FROM BloodRequest br WHERE br.hospital.id = :hospitalId")
    long sumUnitsRequiredByHospitalId(@org.springframework.data.repository.query.Param("hospitalId") Long hospitalId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(br.unitsFulfilled), 0) FROM BloodRequest br WHERE br.hospital.id = :hospitalId")
    long sumUnitsFulfilledByHospitalId(@org.springframework.data.repository.query.Param("hospitalId") Long hospitalId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(br.unitsFulfilled), 0) FROM BloodRequest br")
    long sumTotalUnitsFulfilled();

    @org.springframework.data.jpa.repository.Query("SELECT br FROM BloodRequest br " +
                                                   "JOIN FETCH br.hospital h " +
                                                   "JOIN FETCH br.province p " +
                                                   "JOIN FETCH br.city c " +
                                                   "WHERE br.status IN :statuses ORDER BY br.createdAt DESC")
    List<BloodRequest> findByStatusInWithDetails(@org.springframework.data.repository.query.Param("statuses") java.util.Collection<RequestStatus> statuses);

    /** Find request by ID with eager-loaded hospital, province, and city. */
    @org.springframework.data.jpa.repository.Query("SELECT br FROM BloodRequest br " +
                                                   "JOIN FETCH br.hospital h " +
                                                   "JOIN FETCH br.province p " +
                                                   "JOIN FETCH br.city c " +
                                                   "WHERE br.id = :id")
    java.util.Optional<BloodRequest> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * Search and filter blood requests with pagination for the administrator.
     */
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT br FROM BloodRequest br " +
                    "JOIN FETCH br.hospital h " +
                    "JOIN FETCH br.province p " +
                    "JOIN FETCH br.city c " +
                    "WHERE (:query IS NULL OR :query = '' " +
                    "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "       OR LOWER(br.hospitalNotes) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                    "  AND (:status IS NULL OR br.status = :status) " +
                    "  AND (:urgency IS NULL OR br.urgency = :urgency) " +
                    "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup) " +
                    "ORDER BY br.createdAt DESC",
            countQuery = "SELECT COUNT(br) FROM BloodRequest br " +
                         "JOIN br.hospital h " +
                         "WHERE (:query IS NULL OR :query = '' " +
                         "       OR LOWER(h.hospitalName) LIKE LOWER(CONCAT('%', :query, '%')) " +
                         "       OR LOWER(br.hospitalNotes) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                         "  AND (:status IS NULL OR br.status = :status) " +
                         "  AND (:urgency IS NULL OR br.urgency = :urgency) " +
                         "  AND (:bloodGroup IS NULL OR br.bloodGroup = :bloodGroup)")
    Page<BloodRequest> searchAndFilter(
            @org.springframework.data.repository.query.Param("query") String query,
            @org.springframework.data.repository.query.Param("status") RequestStatus status,
            @org.springframework.data.repository.query.Param("urgency") RequestUrgency urgency,
            @org.springframework.data.repository.query.Param("bloodGroup") BloodGroup bloodGroup,
            Pageable pageable);
}
