package com.donornexa.hospital.service;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.hospital.dto.HospitalAccountStatusActionDTO;
import com.donornexa.hospital.dto.HospitalProfileDTO;
import com.donornexa.hospital.dto.HospitalVerificationActionDTO;
import com.donornexa.hospital.dto.HospitalVerificationStatus;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing administrator-driven hospital verification and account status workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalVerificationService {

    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Search and filter registered hospitals with pagination for the administrator.
     */
    @Transactional(readOnly = true)
    public Page<Hospital> searchHospitals(String query,
                                          HospitalVerificationStatus verificationStatus,
                                          AccountStatus accountStatus,
                                          Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String statusStr = (verificationStatus != null) ? verificationStatus.name() : null;

        return hospitalRepository.searchAndFilter(cleanQuery, statusStr, accountStatus, pageable);
    }

    /**
     * Review and process a verification decision (APPROVE or REJECT) for a hospital.
     *
     * @param hospitalId Hospital primary key ID
     * @param dto        Decision DTO containing action and optional/mandatory rejection reason
     * @param adminEmail Authenticated administrator email
     * @return Updated {@link Hospital}
     */
    @Transactional
    public Hospital verifyHospital(Long hospitalId, HospitalVerificationActionDTO dto, String adminEmail) {
        Hospital hospital = hospitalRepository.findByIdWithDetails(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException("No hospital found with ID: " + hospitalId));

        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);
        User hospitalUser = hospital.getUser();

        String action = dto.getAction().trim().toUpperCase();

        if ("APPROVE".equals(action)) {
            if (Boolean.TRUE.equals(hospital.getVerified())) {
                throw new HospitalOperationException("Hospital '" + hospital.getHospitalName() + "' is already verified.");
            }

            hospital.setVerified(Boolean.TRUE);
            hospital.setRejectionReason(null);
            hospitalUser.setAccountStatus(AccountStatus.ACTIVE);
            hospitalUser.setEnabled(Boolean.TRUE);

            recordAuditLog(adminUser, AuditAction.VERIFY, "Hospital", String.valueOf(hospitalId),
                    "PENDING", "VERIFIED",
                    "Hospital '" + hospital.getHospitalName() + "' (Reg: " + hospital.getRegistrationNumber()
                            + ") verification APPROVED by administrator " + adminEmail);

            sendNotification(hospitalUser, "Hospital Verification Approved",
                    "Congratulations! Your hospital profile for '" + hospital.getHospitalName()
                            + "' has been verified and your account is now ACTIVE.",
                    NotificationType.SYSTEM_ALERT);

            log.info("Hospital ID {} verified by admin {}", hospitalId, adminEmail);

        } else if ("REJECT".equals(action)) {
            if (Boolean.TRUE.equals(hospital.getVerified())) {
                throw new HospitalOperationException("Cannot reject an already verified hospital. Suspend the account instead.");
            }
            if (hospital.getRejectionReason() != null && !hospital.getRejectionReason().isBlank()) {
                throw new HospitalOperationException("Hospital verification has already been rejected.");
            }
            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new HospitalOperationException("A specific rejection reason is required when rejecting a hospital.");
            }

            String reason = dto.getRejectionReason().trim();
            hospital.setVerified(Boolean.FALSE);
            hospital.setRejectionReason(reason);
            hospitalUser.setAccountStatus(AccountStatus.INACTIVE);
            hospitalUser.setEnabled(Boolean.FALSE);

            recordAuditLog(adminUser, AuditAction.VERIFY, "Hospital", String.valueOf(hospitalId),
                    "PENDING", "REJECTED",
                    "Hospital '" + hospital.getHospitalName() + "' verification REJECTED by administrator "
                            + adminEmail + ". Reason: " + reason);

            sendNotification(hospitalUser, "Hospital Verification Not Approved",
                    "Your hospital verification for '" + hospital.getHospitalName()
                            + "' was not approved. Reason: " + reason,
                    NotificationType.SYSTEM_ALERT);

            log.info("Hospital ID {} rejected by admin {}. Reason: {}", hospitalId, adminEmail, reason);

        } else {
            throw new HospitalOperationException("Unsupported verification action: " + action);
        }

        userRepository.save(hospitalUser);
        return hospitalRepository.save(hospital);
    }

    /**
     * Update a hospital's account lifecycle status (ACTIVE, SUSPENDED, INACTIVE).
     *
     * @param hospitalId Hospital primary key ID
     * @param dto        Account status action DTO
     * @param adminEmail Authenticated administrator email
     * @return Updated {@link Hospital}
     */
    @Transactional
    public Hospital updateAccountStatus(Long hospitalId, HospitalAccountStatusActionDTO dto, String adminEmail) {
        Hospital hospital = hospitalRepository.findByIdWithDetails(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException("No hospital found with ID: " + hospitalId));

        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);
        User hospitalUser = hospital.getUser();
        AccountStatus oldStatus = hospitalUser.getAccountStatus();
        AccountStatus newStatus = dto.getAccountStatus();

        if (oldStatus == newStatus) {
            return hospital;
        }

        hospitalUser.setAccountStatus(newStatus);
        hospitalUser.setEnabled(newStatus == AccountStatus.ACTIVE || newStatus == AccountStatus.PENDING_VERIFICATION);

        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "Hospital", String.valueOf(hospitalId),
                oldStatus.name(), newStatus.name(),
                "Hospital '" + hospital.getHospitalName() + "' account status changed from "
                        + oldStatus + " to " + newStatus + " by administrator " + adminEmail);

        sendNotification(hospitalUser, "Account Status Updated",
                "Your account status has been changed to " + newStatus.name() + " by the platform administration.",
                NotificationType.SYSTEM_ALERT);

        userRepository.save(hospitalUser);
        log.info("Hospital ID {} status changed from {} to {} by admin {}", hospitalId, oldStatus, newStatus, adminEmail);
        return hospital;
    }

    // -------------------------------------------------------------------------
    // Statistical metrics for Admin Dashboard
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public long getTotalHospitalsCount() {
        return hospitalRepository.count();
    }

    @Transactional(readOnly = true)
    public long getPendingVerificationCount() {
        return hospitalRepository.countPendingVerification();
    }

    @Transactional(readOnly = true)
    public long getVerifiedHospitalsCount() {
        return hospitalRepository.countByVerified(Boolean.TRUE);
    }

    @Transactional(readOnly = true)
    public long getRejectedHospitalsCount() {
        return hospitalRepository.countRejectedVerification();
    }

    @Transactional(readOnly = true)
    public long getSuspendedHospitalsCount() {
        return hospitalRepository.countByUserAccountStatus(AccountStatus.SUSPENDED);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void recordAuditLog(User actor, AuditAction action, String entityName, String entityId,
                                String oldValue, String newValue, String description) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setActor(actor);
            logEntry.setAction(action);
            logEntry.setEntityName(entityName);
            logEntry.setEntityId(entityId);
            logEntry.setOldValue(oldValue);
            logEntry.setNewValue(newValue);
            logEntry.setDescription(description);

            String ip = "127.0.0.1";
            try {
                org.springframework.web.context.request.ServletRequestAttributes attrs =
                        (org.springframework.web.context.request.ServletRequestAttributes)
                                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attrs != null && attrs.getRequest() != null) {
                    jakarta.servlet.http.HttpServletRequest req = attrs.getRequest();
                    String xff = req.getHeader("X-Forwarded-For");
                    ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
                }
            } catch (Exception ignored) {}
            logEntry.setIpAddress(ip != null ? ip : "127.0.0.1");

            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to record audit log for action {}: {}", action, ex.getMessage());
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
            notificationRepository.save(notification);
        } catch (Exception ex) {
            log.error("Failed to dispatch notification to {}: {}", recipient.getEmail(), ex.getMessage());
        }
    }
}
