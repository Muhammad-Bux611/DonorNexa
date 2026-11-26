package com.donornexa.hospital.service;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.common.enums.Role;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.entity.User;
import com.donornexa.user.repository.UserRepository;

import com.donornexa.bloodrequest.dto.CreateBloodRequestDTO;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.exception.HospitalNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service managing hospital blood request lifecycle, creation, viewing, and cancellation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalBloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final com.donornexa.notification.service.NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<BloodRequest> getRequestsForHospital(String hospitalEmail, Pageable pageable) {
        Hospital hospital = getHospitalByEmail(hospitalEmail);
        return bloodRequestRepository.findByHospitalId(hospital.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public BloodRequest getRequestByIdForHospital(Long requestId, String hospitalEmail) {
        Hospital hospital = getHospitalByEmail(hospitalEmail);
        BloodRequest request = bloodRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + requestId));

        if (!request.getHospital().getId().equals(hospital.getId())) {
            throw new AccessDeniedException("Access denied. You do not own this blood request.");
        }
        return request;
    }

    @Transactional
    public BloodRequest createRequest(String hospitalEmail, CreateBloodRequestDTO dto) {
        Hospital hospital = getHospitalByEmail(hospitalEmail);

        if (!Boolean.TRUE.equals(hospital.getVerified())) {
            throw new AccessDeniedException("Hospital verification is pending or rejected. Only verified hospitals can raise blood requests.");
        }

        // Anti-misuse quota check: Max 3 simultaneous open CRITICAL requests per hospital
        if (dto.getUrgency() == com.donornexa.common.enums.RequestUrgency.CRITICAL) {
            long openCriticalCount = bloodRequestRepository.countByHospitalIdAndUrgencyAndStatusIn(
                    hospital.getId(),
                    com.donornexa.common.enums.RequestUrgency.CRITICAL,
                    List.of(RequestStatus.PENDING, RequestStatus.PENDING_ADMIN_APPROVAL, RequestStatus.MATCHING, RequestStatus.PARTIALLY_FULFILLED)
            );
            if (openCriticalCount >= 3) {
                throw new IllegalStateException("Anti-misuse quota exceeded: Hospital already has " + openCriticalCount +
                        " active CRITICAL blood requests. You cannot maintain more than 3 simultaneous open CRITICAL requests.");
            }
        }

        Province province = provinceRepository.findById(dto.getProvinceId())
                .orElseThrow(() -> new NoSuchElementException("Province not found with ID: " + dto.getProvinceId()));
        City city = cityRepository.findById(dto.getCityId())
                .orElseThrow(() -> new NoSuchElementException("City not found with ID: " + dto.getCityId()));

        if (!city.getProvince().getId().equals(province.getId())) {
            throw new IllegalArgumentException("Selected city does not belong to the selected province.");
        }

        BloodRequest request = new BloodRequest();
        request.setHospital(hospital);
        request.setBloodGroup(dto.getBloodGroup());
        request.setUnitsRequired(dto.getUnitsRequired());
        request.setUnitsFulfilled(0);
        request.setUrgency(dto.getUrgency());
        request.setStatus(RequestStatus.PENDING_ADMIN_APPROVAL);
        request.setRequiredBy(dto.getRequiredBy() != null ? dto.getRequiredBy() : LocalDateTime.now().plusDays(2));
        request.setProvince(province);
        request.setCity(city);
        request.setHospitalNotes(dto.getHospitalNotes());

        BloodRequest saved = bloodRequestRepository.save(request);

        // Audit Log
        recordAuditLog(hospital.getUser(), AuditAction.CREATE, "BloodRequest", String.valueOf(saved.getId()),
                "Hospital " + hospital.getHospitalName() + " created blood request #" + saved.getId()
                        + " for " + saved.getUnitsRequired() + " unit(s) of " + saved.getBloodGroup()
                        + " (" + saved.getUrgency() + "). Pending admin approval.");

        // Notify System Administrators of pending blood request
        try {
            List<User> admins = userRepository.findByRole(com.donornexa.common.enums.Role.ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "New Blood Request #" + saved.getId() + " Awaiting Approval",
                        hospital.getHospitalName() + " has requested " + saved.getUnitsRequired() + " unit(s) of "
                                + saved.getBloodGroup() + " (" + saved.getUrgency() + ").",
                        com.donornexa.common.enums.NotificationType.BLOOD_REQUEST
                );
            }
        } catch (Exception ex) {
            log.warn("Failed to notify admins of new blood request: {}", ex.getMessage());
        }

        try {
            if (hospital.getUser() != null) {
                notificationService.createNotification(hospital.getUser(),
                        "Blood Request Raised (#" + saved.getId() + ")",
                        "You successfully created blood request #" + saved.getId() + " for " + saved.getUnitsRequired() + " unit(s) of " + saved.getBloodGroup() + " blood.",
                        com.donornexa.common.enums.NotificationType.BLOOD_REQUEST);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch blood request notification to hospital: {}", ex.getMessage());
        }

        log.info("Blood request #{} created by hospital {} ({})", saved.getId(), hospital.getHospitalName(), hospitalEmail);
        return saved;
    }

    @Transactional
    public BloodRequest cancelRequest(Long requestId, String hospitalEmail) {
        BloodRequest request = getRequestByIdForHospital(requestId, hospitalEmail);

        if (request.getStatus() == RequestStatus.FULFILLED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Request #" + requestId + " cannot be cancelled because it is already " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        BloodRequest saved = bloodRequestRepository.save(request);

        // Trust score penalty on hospital cancellation (deduct 5 points, minimum 0)
        Hospital hospital = request.getHospital();
        int oldTrustScore = hospital.getTrustScore() != null ? hospital.getTrustScore() : 100;
        int newTrustScore = Math.max(0, oldTrustScore - 5);
        hospital.setTrustScore(newTrustScore);
        hospitalRepository.save(hospital);

        recordAuditLog(request.getHospital().getUser(), AuditAction.STATUS_CHANGE, "BloodRequest", String.valueOf(requestId),
                "status=" + request.getStatus(), "status=CANCELLED",
                "Hospital " + request.getHospital().getHospitalName() + " cancelled blood request #" + requestId);

        recordAuditLog(request.getHospital().getUser(), AuditAction.TRUST_SCORE_UPDATE, "Hospital", String.valueOf(hospital.getId()),
                "trustScore=" + oldTrustScore, "trustScore=" + newTrustScore,
                "Hospital trust score decreased by 5 points following cancellation of blood request #" + requestId);

        try {
            if (request.getHospital().getUser() != null) {
                notificationService.createNotification(request.getHospital().getUser(),
                        "Blood Request Cancelled (#" + requestId + ")",
                        "Blood request #" + requestId + " has been cancelled.",
                        com.donornexa.common.enums.NotificationType.BLOOD_REQUEST);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch cancel notification to hospital: {}", ex.getMessage());
        }

        log.info("Blood request #{} cancelled by hospital {}", requestId, hospitalEmail);
        return saved;
    }

    private Hospital getHospitalByEmail(String email) {
        return hospitalRepository.findByUserEmailWithDetails(email.toLowerCase().trim())
                .orElseThrow(() -> new HospitalNotFoundException("Hospital profile not found for user: " + email));
    }

    private void recordAuditLog(User actor, AuditAction action, String entityName, String entityId, String description) {
        recordAuditLog(actor, action, entityName, entityId, null, null, description);
    }

    private void recordAuditLog(User actor, AuditAction action, String entityName, String entityId, String oldValue, String newValue, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setActor(actor);
            log.setAction(action);
            log.setEntityName(entityName);
            log.setEntityId(entityId);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setDescription(description);
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception ex) {
            log.error("Failed to record audit log: {}", ex.getMessage());
        }
    }
}
