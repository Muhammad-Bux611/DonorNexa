package com.donornexa.admin.service;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Service managing administrative oversight and status updates for blood requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;

    @Transactional(readOnly = true)
    public Page<BloodRequest> searchBloodRequests(String query,
                                                  RequestStatus status,
                                                  RequestUrgency urgency,
                                                  BloodGroup bloodGroup,
                                                  Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return bloodRequestRepository.searchAndFilter(cleanQuery, status, urgency, bloodGroup, pageable);
    }

    @Transactional(readOnly = true)
    public BloodRequest getBloodRequestById(Long id) {
        return bloodRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + id));
    }

    @Transactional
    public BloodRequest approveBloodRequest(Long requestId, String adminNotes, String adminEmail) {
        BloodRequest request = getBloodRequestById(requestId);
        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);

        request.setStatus(RequestStatus.APPROVED);
        request.setAdminNotes(adminNotes != null && !adminNotes.isBlank() ? adminNotes.trim() : "Approved by administrator.");
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(adminEmail);

        BloodRequest saved = bloodRequestRepository.save(request);

        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "BloodRequest", String.valueOf(requestId),
                "Blood request #" + requestId + " was APPROVED by administrator " + adminEmail);

        User hospitalUser = request.getHospital().getUser();
        if (hospitalUser != null) {
            sendNotification(hospitalUser, "Blood Request #" + requestId + " Approved",
                    "Your blood request #" + requestId + " for " + request.getUnitsRequired() + " unit(s) of "
                            + request.getBloodGroup() + " has been APPROVED by administrator. You can now execute donor matching.",
                    NotificationType.BLOOD_REQUEST);
        }

        log.info("Blood request #{} approved by admin {}", requestId, adminEmail);
        return saved;
    }

    @Transactional
    public BloodRequest rejectBloodRequest(Long requestId, String rejectionReason, String adminEmail) {
        BloodRequest request = getBloodRequestById(requestId);
        if (request.getStatus() == RequestStatus.FULFILLED) {
            throw new IllegalStateException("Fulfilled blood requests cannot be rejected.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled blood requests cannot be rejected.");
        }
        if (request.getStatus() == RequestStatus.EXPIRED) {
            throw new IllegalStateException("Expired blood requests cannot be rejected.");
        }
        if (request.getStatus() == RequestStatus.REJECTED) {
            throw new IllegalStateException("Blood request #" + requestId + " has already been rejected.");
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("A specific rejection reason is required when rejecting a blood request.");
        }

        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);

        request.setStatus(RequestStatus.REJECTED);
        String reason = rejectionReason.trim();
        request.setAdminNotes(reason);

        BloodRequest saved = bloodRequestRepository.save(request);

        // Cancel any pending or invited donor matches to prevent further allocation
        if (requestMatchRepository != null) {
            java.util.List<com.donornexa.matching.entity.RequestMatch> matches = requestMatchRepository.findByBloodRequestId(requestId);
            for (com.donornexa.matching.entity.RequestMatch match : matches) {
                if (match.getMatchStatus() == com.donornexa.common.enums.MatchStatus.PENDING ||
                    match.getMatchStatus() == com.donornexa.common.enums.MatchStatus.INVITED) {
                    match.setMatchStatus(com.donornexa.common.enums.MatchStatus.CANCELLED);
                    requestMatchRepository.save(match);
                }
            }
        }

        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "BloodRequest", String.valueOf(requestId),
                "Blood request #" + requestId + " was REJECTED by administrator " + adminEmail + ". Reason: " + reason);

        User hospitalUser = request.getHospital().getUser();
        if (hospitalUser != null) {
            sendNotification(hospitalUser, "Blood Request #" + requestId + " Rejected",
                    "Your blood request #" + requestId + " for " + request.getBloodGroup()
                            + " has been REJECTED by administrator. Reason: " + reason,
                    NotificationType.BLOOD_REQUEST);
        }

        log.info("Blood request #{} rejected by admin {}: {}", requestId, adminEmail, reason);
        return saved;
    }

    @Transactional
    public BloodRequest updateStatus(Long requestId, RequestStatus newStatus, String adminEmail) {
        BloodRequest request = getBloodRequestById(requestId);
        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);

        RequestStatus oldStatus = request.getStatus();
        if (oldStatus == newStatus) {
            return request;
        }

        request.setStatus(newStatus);
        BloodRequest saved = bloodRequestRepository.save(request);

        // Record Audit Log
        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "BloodRequest", String.valueOf(requestId),
                "Blood request #" + requestId + " for " + request.getBloodGroup() + " at "
                        + request.getHospital().getHospitalName() + " status transitioned from "
                        + oldStatus + " to " + newStatus + " by administrator " + adminEmail);

        // Notify Hospital
        User hospitalUser = request.getHospital().getUser();
        if (hospitalUser != null) {
            sendNotification(hospitalUser, "Blood Request Status Updated",
                    "Your blood request #" + requestId + " for " + request.getBloodGroup()
                            + " has been updated to " + newStatus.name() + " by system administration.",
                    NotificationType.BLOOD_REQUEST);
        }

        log.info("Blood request {} status changed from {} to {} by admin {}", requestId, oldStatus, newStatus, adminEmail);
        return saved;
    }

    private void recordAuditLog(User actor, AuditAction action, String entityName, String entityId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setActor(actor);
            auditLog.setAction(action);
            auditLog.setEntityName(entityName);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to write audit log for blood request action: {}", ex.getMessage());
        }
    }

    private void sendNotification(User recipient, String title, String message, NotificationType type) {
        try {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setNotificationType(type);
            notification.setNotificationStatus(NotificationStatus.UNREAD);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception ex) {
            log.error("Failed to send notification for blood request: {}", ex.getMessage());
        }
    }
}
