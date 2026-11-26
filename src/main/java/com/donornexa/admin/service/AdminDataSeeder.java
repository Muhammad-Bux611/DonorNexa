package com.donornexa.admin.service;

import com.donornexa.common.enums.Role;
import com.donornexa.audit.entity.AuditLog;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Automatically provisions the primary platform administrator account upon application startup
 * if no administrator account exists in the database.
 *
 * <p>Idempotent and secure:
 * <ul>
 *   <li>Generates a strong, random temporary administrator password.</li>
 *   <li>Encodes password using BCrypt â€” never stored as plaintext in MySQL.</li>
 *   <li>Enforces mandatory first-login password change flag (mustChangePassword = true).</li>
 *   <li>Writes credentials once to local file donornexa-admin-credentials.txt (ignored in .gitignore).</li>
 *   <li>Never overwrites existing administrator accounts or passwords on restart.</li>
 * </ul>
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class AdminDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    @Value("${donornexa.admin.initial-password:}")
    private String configuredAdminPassword;

    @Value("${donornexa.admin.force-first-login-change:true}")
    private boolean forceFirstLoginChange;

    public static final String DEFAULT_ADMIN_EMAIL = "admin@donornexa.com";
    public static final String DEFAULT_ADMIN_PHONE = "03000000000";

    private static String latestGeneratedAdminPassword = null;

    /**
     * Retrieves the temporary password generated during the initial seeding run.
     */
    public static String getLatestGeneratedAdminPassword() {
        return latestGeneratedAdminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.findByRole(Role.ADMIN).isEmpty()) {
            log.info("Administrator accounts already exist. Skipping initial admin seeding.");
            return;
        }

        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            log.warn("Default admin email {} already exists with another role. Skipping admin seeding.", DEFAULT_ADMIN_EMAIL);
            return;
        }

        String tempPassword;
        if (configuredAdminPassword != null && !configuredAdminPassword.isBlank()) {
            tempPassword = configuredAdminPassword.trim();
        } else {
            tempPassword = generateSecureTemporaryPassword();
        }
        latestGeneratedAdminPassword = tempPassword;

        User admin = new User();
        admin.setEmail(DEFAULT_ADMIN_EMAIL);
        admin.setFullName("System Administrator");
        admin.setPhoneNumber(DEFAULT_ADMIN_PHONE);
        admin.setPasswordHash(passwordEncoder.encode(tempPassword));
        admin.setRole(Role.ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setEnabled(Boolean.TRUE);
        admin.setMustChangePassword(forceFirstLoginChange ? Boolean.TRUE : Boolean.FALSE);
        User savedAdmin = userRepository.save(admin);

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(savedAdmin);
        auditLog.setAction(AuditAction.CREATE);
        auditLog.setEntityName("User");
        auditLog.setEntityId(String.valueOf(savedAdmin.getId()));
        auditLog.setDescription("Initial system administrator provisioned with mandatory temporary password.");
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);

        writeCredentialsFile(DEFAULT_ADMIN_EMAIL, tempPassword);

        System.out.println("================================================================================");
        System.out.println("[DONORNEXA SECURITY] INITIAL ADMINISTRATOR ACCOUNT PROVISIONED");
        System.out.println("Email:           " + DEFAULT_ADMIN_EMAIL);
        System.out.println("Temp Password:   " + tempPassword);
        System.out.println("Login URL:       http://localhost:8080/admin/login");
        System.out.println("Action:          First-login password change is mandatory.");
        System.out.println("Credentials in:  donornexa-admin-credentials.txt");
        System.out.println("================================================================================");

        log.info("Initial system administrator initialized: {}", DEFAULT_ADMIN_EMAIL);
    }

    private String generateSecureTemporaryPassword() {
        final String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        final String lower = "abcdefghijkmnpqrstuvwxyz";
        final String digits = "23456789";
        final String special = "!@#$*=";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append("DN#Admin!");
        for (int i = 0; i < 2; i++) sb.append(upper.charAt(random.nextInt(upper.length())));
        for (int i = 0; i < 2; i++) sb.append(lower.charAt(random.nextInt(lower.length())));
        for (int i = 0; i < 2; i++) sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        return sb.toString();
    }

    private void writeCredentialsFile(String email, String password) {
        try {
            Path path = Paths.get("donornexa-admin-credentials.txt");
            String content = "================================================================================\n"
                    + "DONORNEXA â€” INITIAL ADMINISTRATOR ACCOUNT CREDENTIALS\n"
                    + "Generated at: " + LocalDateTime.now() + "\n"
                    + "================================================================================\n"
                    + "Email:         " + email + "\n"
                    + "Temp Password: " + password + "\n"
                    + "Login URL:     http://localhost:8080/admin/login\n"
                    + "Dashboard URL: http://localhost:8080/admin/dashboard\n\n"
                    + "SECURITY NOTICE:\n"
                    + "1. This temporary password requires a mandatory password change on first login.\n"
                    + "2. Record these credentials in a secure location.\n"
                    + "3. You may safely delete this file once recorded.\n"
                    + "================================================================================\n";
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Initial administrator credentials recorded at {}", path.toAbsolutePath());
        } catch (Exception ex) {
            log.warn("Could not write credentials file: {}", ex.getMessage());
        }
    }
}
