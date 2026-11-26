package com.donornexa.admin.service;

import com.donornexa.admin.dto.AdminCreateUserDTO;
import com.donornexa.common.enums.Role;
import com.donornexa.audit.entity.AuditLog;
import com.donornexa.notification.entity.Notification;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Service managing user account administration, lifecycle transitions,
 * search, and audit recording.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Search and filter registered users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Role role, AccountStatus accountStatus, Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return userRepository.searchAndFilter(cleanQuery, role, accountStatus, pageable);
    }

    /**
     * Retrieve user by ID.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User account not found with ID: " + id));
    }

    /**
     * Create a new administrator account by an authenticated administrator.
     */
    @Transactional
    public User createAdministrator(AdminCreateUserDTO dto, String creatorEmail) {
        String cleanEmail = dto.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account is already registered with email: " + cleanEmail);
        }

        String cleanPhone = dto.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumber(cleanPhone)) {
            throw new IllegalArgumentException("An account is already registered with phone number: " + cleanPhone);
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User adminActor = userRepository.findByEmail(creatorEmail.toLowerCase().trim()).orElse(null);

        User newUser = new User();
        newUser.setFullName(dto.getFullName().trim());
        newUser.setEmail(cleanEmail);
        newUser.setPhoneNumber(cleanPhone);
        newUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        newUser.setRole(Role.ADMIN);
        newUser.setAccountStatus(AccountStatus.ACTIVE);
        newUser.setEnabled(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);

        recordAuditLog(adminActor, AuditAction.CREATE, "User", String.valueOf(savedUser.getId()),
                "Administrator account created for " + cleanEmail + " by administrator " + creatorEmail);

        log.info("New administrator account created for {} by {}", cleanEmail, creatorEmail);
        return savedUser;
    }

    /**
     * Transition a user's account status (ACTIVE, SUSPENDED, INACTIVE).
     * Prevents administrators from suspending or deactivating their own account.
     */
    @Transactional
    public User updateUserStatus(Long userId, AccountStatus newStatus, String adminEmail) {
        User targetUser = getUserById(userId);
        User adminUser = userRepository.findByEmail(adminEmail.toLowerCase().trim()).orElse(null);

        // Self-lockout prevention check
        if (targetUser.getEmail().equalsIgnoreCase(adminEmail.trim())) {
            throw new IllegalArgumentException("Administrators cannot suspend, deactivate, or modify their own account status.");
        }

        AccountStatus oldStatus = targetUser.getAccountStatus();
        if (oldStatus == newStatus) {
            return targetUser;
        }

        targetUser.setAccountStatus(newStatus);
        targetUser.setEnabled(newStatus == AccountStatus.ACTIVE);
        User savedUser = userRepository.save(targetUser);

        // Record Audit Log
        recordAuditLog(adminUser, AuditAction.STATUS_CHANGE, "User", String.valueOf(userId),
                "User account '" + targetUser.getEmail() + "' status changed from "
                        + oldStatus + " to " + newStatus + " by administrator " + adminEmail);

        // Send System Notification to User
        sendNotification(targetUser, "Account Status Updated",
                "Your DonorNexa account status has been changed to " + newStatus.name()
                        + " by platform administration.",
                NotificationType.SYSTEM_ALERT);

        log.info("User {} account status transitioned from {} to {} by admin {}",
                targetUser.getEmail(), oldStatus, newStatus, adminEmail);

        return savedUser;
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
            log.error("Failed to write audit log for user action: {}", ex.getMessage());
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
            log.error("Failed to send notification to user {}: {}", recipient.getEmail(), ex.getMessage());
        }
    }
}
