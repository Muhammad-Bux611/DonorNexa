package com.donornexa.admin.service;

import com.donornexa.donor.dto.DonorAccountStatusActionDTO;
import com.donornexa.donor.dto.DonorVerificationActionDTO;
import com.donornexa.donor.dto.DonorVerificationStatus;
import com.donornexa.common.exception.DonorProfileException;
import com.donornexa.audit.entity.AuditLog;
import com.donornexa.donor.entity.Donor;
import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service managing administrator-driven donor verification and account status workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDonorVerificationService {

    private final DonorRepository donorRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Search and filter registered donors with pagination for the administrator.
     */
    @Transactional(readOnly = true)
    public Page<Donor> searchDonors(String query,
                                    DonorVerificationStatus verificationStatus,
                                    BloodGroup bloodGroup,
                                    AccountStatus accountStatus,
                                    Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String statusStr = (verificationStatus != null) ? verificationStatus.name() : null;

        return donorRepository.searchAndFilter(cleanQuery, statusStr, bloodGroup, accountStatus, pageable);
    }

    /**
     * Retrieve donor entity by primary key with eager-loaded user, province, and city.
     */
    @Transactional(readOnly = true)
    public Donor getDonorById(Long donorId) {
        return donorRepository.findByIdWithDetails(donorId)
                .orElseThrow(() -> new DonorProfileException("No donor profile found with ID: " + donorId));
    }

    /**
     * Review and process a verification decision (APPROVE or REJECT) for a donor.
     *
     * @param donorId    Donor primary key ID
     * @param dto        Decision DTO containing action and optional/mandatory rejection reason
     * @param adminEmail Authenticated administrator email
     * @return Updated {@link Donor}
     */
    @Transactional
    public Donor verifyDonor(Long donorId, DonorVerificationActionDTO dto, String adminEmail) {
        Donor donor = donorRepository.findByIdWithDetails(donorId)
                .orElseThrow(() -> new DonorProfileException("No donor profile found with ID: " + donorId));

        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);
        User donorUser = donor.getUser();

        String action = dto.getAction().trim().toUpperCase();

        if ("APPROVE".equals(action)) {
            if (Boolean.TRUE.equals(donor.getVerified())) {
                throw new DonorProfileException("Donor '" + donorUser.getFullName() + "' is already verified.");
            }

            donor.setVerified(Boolean.TRUE);
            donor.setRejectionReason(null);
            donorUser.setAccountStatus(AccountStatus.ACTIVE);
            donorUser.setEnabled(Boolean.TRUE);

            recordAuditLog(adminUser, AuditAction.VERIFY, "Donor", String.valueOf(donorId),
                    "Donor '" + donorUser.getFullName() + "' (Blood Group: " + donor.getBloodGroup()
                            + ") verification APPROVED by administrator " + adminEmail);

            sendNotification(donorUser, "Donor Verification Approved",
                    "Congratulations! Your donor profile has been verified and your account is now ACTIVE. Thank you for your commitment to saving lives.",
                    NotificationType.SYSTEM_ALERT);

            log.info("Donor ID {} verified by admin {}", donorId, adminEmail);

        } else if ("REJECT".equals(action)) {
            if (Boolean.TRUE.equals(donor.getVerified())) {
                throw new DonorProfileException("Cannot reject an already verified donor. Suspend the account instead.");
            }
            if (donor.getRejectionReason() != null && !donor.getRejectionReason().isBlank()) {
                throw new DonorProfileException("Donor verification has already been rejected.");
            }
            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new DonorProfileException("A specific rejection reason is required when rejecting a donor.");
            }

            String reason = dto.getRejectionReason().trim();
            donor.setVerified(Boolean.FALSE);
            donor.setRejectionReason(reason);
            donorUser.setAccountStatus(AccountStatus.INACTIVE);
            donorUser.setEnabled(Boolean.FALSE);

            recordAuditLog(adminUser, AuditAction.VERIFY, "Donor", String.valueOf(donorId),
                    "Donor '" + donorUser.getFullName() + "' verification REJECTED by administrator "
                            + adminEmail + ". Reason: " + reason);

            sendNotification(donorUser, "Donor Verification Rejected",
                    "Your donor verification was not approved for the following reason: " + reason
                            + ". Please contact support or update your profile details.",
                    NotificationType.SYSTEM_ALERT);

            log.info("Donor ID {} verification rejected by admin {}: {}", donorId, adminEmail, reason);

        } else {
            throw new DonorProfileException("Unsupported verification action: " + action);
        }

        userRepository.save(donorUser);
        return donorRepository.save(donor);
    }

    /**
     * Transition a donor user's account status (ACTIVE, SUSPENDED, INACTIVE).
     *
     * @param donorId    Donor primary key ID
     * @param dto        Account status action DTO
     * @param adminEmail Authenticated administrator email
     * @return Updated {@link Donor}
     */
    @Transactional
    public Donor updateAccountStatus(Long donorId, DonorAccountStatusActionDTO dto, String adminEmail) {
        Donor donor = donorRepository.findByIdWithDetails(donorId)
                .orElseThrow(() -> new DonorProfileException("No donor profile found with ID: " + donorId));

        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);
        User donorUser = donor.getUser();
        AccountStatus oldStatus = donorUser.getAccountStatus();
        AccountStatus newStatus = dto.getAccountStatus();

        if (oldStatus == newStatus) {
            return donor;
        }

        donorUser.setAccountStatus(newStatus);
        donorUser.setEnabled(newStatus == AccountStatus.ACTIVE);
        userRepository.save(donorUser);

        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "Donor", String.valueOf(donorId),
                "Donor '" + donorUser.getFullName() + "' account status changed from "
                        + oldStatus + " to " + newStatus + " by administrator " + adminEmail);

        sendNotification(donorUser, "Account Status Updated",
                "Your account status has been changed to " + newStatus.name() + " by system administration.",
                NotificationType.SYSTEM_ALERT);

        log.info("Donor ID {} account status transitioned from {} to {} by admin {}",
                donorId, oldStatus, newStatus, adminEmail);

        return donor;
    }

    public long getTotalDonorsCount() {
        return donorRepository.count();
    }

    public long getPendingVerificationCount() {
        return donorRepository.countPendingVerification();
    }

    public long getVerifiedDonorsCount() {
        return donorRepository.countByVerified(Boolean.TRUE);
    }

    public long getRejectedDonorsCount() {
        return donorRepository.countRejectedVerification();
    }

    public long getSuspendedDonorsCount() {
        return donorRepository.countByUserAccountStatus(AccountStatus.SUSPENDED);
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
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception ex) {
            log.error("Failed to dispatch notification to {}: {}", recipient.getEmail(), ex.getMessage());
        }
    }
}
