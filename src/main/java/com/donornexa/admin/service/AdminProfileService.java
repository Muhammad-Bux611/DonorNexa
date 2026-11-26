package com.donornexa.admin.service;

import com.donornexa.admin.dto.AdminPasswordChangeDTO;
import com.donornexa.admin.dto.AdminProfileUpdateDTO;
import com.donornexa.audit.entity.AuditLog;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Service managing administrator personal profile and secure credential updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public User getAdminUser(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NoSuchElementException("Administrator user account not found: " + email));
    }

    @Transactional
    public User updateProfile(String email, AdminProfileUpdateDTO dto) {
        User admin = getAdminUser(email);

        String cleanPhone = dto.getPhoneNumber().trim();
        if (userRepository.existsByPhoneNumberAndIdNot(cleanPhone, admin.getId())) {
            throw new IllegalArgumentException("Phone number is already associated with another account.");
        }

        admin.setFullName(dto.getFullName().trim());
        admin.setPhoneNumber(cleanPhone);
        User saved = userRepository.save(admin);

        recordAuditLog(saved, AuditAction.UPDATE, "User", String.valueOf(saved.getId()),
                "Administrator profile updated (name, contact phone) by " + email);

        log.info("Administrator profile updated for {}", email);
        return saved;
    }

    @Transactional
    public void changePassword(String email, AdminPasswordChangeDTO dto) {
        User admin = getAdminUser(email);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match our records.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be identical to your current password.");
        }

        admin.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(admin);

        recordAuditLog(admin, AuditAction.UPDATE, "User", String.valueOf(admin.getId()),
                "Administrator password securely changed by " + email);

        log.info("Administrator password changed successfully for {}", email);
    }

    @Transactional
    public void changeInitialTemporaryPassword(String email, AdminPasswordChangeDTO dto) {
        changePassword(email, dto);
        User admin = getAdminUser(email);
        admin.setMustChangePassword(Boolean.FALSE);
        userRepository.save(admin);
        log.info("First-login temporary password updated for administrator: {}", email);
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
            log.error("Failed to write audit log for admin profile action: {}", ex.getMessage());
        }
    }
}
