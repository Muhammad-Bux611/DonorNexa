package com.donornexa.admin.controller;

import com.donornexa.audit.entity.AuditLog;
import com.donornexa.admin.entity.Setting;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AuditAction;
import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.admin.repository.SettingRepository;
import com.donornexa.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller displaying and managing platform configuration and system parameters for administrators.
 * Supports configurable donor cooldown days stored in the database.
 */
@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final Environment environment;
    private final SettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AdminSettingsController(Environment environment,
                                   SettingRepository settingRepository,
                                   AuditLogRepository auditLogRepository,
                                   UserRepository userRepository) {
        this.environment = environment;
        this.settingRepository = settingRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewSettings(Model model) {
        Map<String, String> systemParams = new LinkedHashMap<>();
        systemParams.put("Platform Name", "DonorNexa â€” Smart Emergency Blood-Donor Coordination Portal");
        systemParams.put("Platform Slogan", "Connecting Donors. Bridging Lives. Saving Hope.");
        systemParams.put("Application Version", "DonorNexa Production 2026 (CEP Edition)");
        systemParams.put("Java Runtime", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        systemParams.put("Operating System", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        systemParams.put("Active Spring Profile(s)", String.join(", ", environment.getActiveProfiles()));
        systemParams.put("Database Dialect", "Hibernate MySQL 8.0 Dialect / H2 (Test)");
        systemParams.put("Authentication Strategy", "Spring Security Form-Based Session Authentication");
        systemParams.put("Password Hash Function", "BCrypt Strong Password Encoder (Cost factor: 10)");
        systemParams.put("Cross-Site Request Forgery (CSRF)", "Enabled (Standard Token Verification)");
        systemParams.put("Server Session Policy", "Stateful Monolithic Sessions with Secure HTTP-Only Cookies");

        int cooldownDays = settingRepository.getIntSetting("donor.cooldown.days", 90);

        model.addAttribute("systemParams", systemParams);
        model.addAttribute("cooldownDays", cooldownDays);
        return "admin/settings";
    }

    @PostMapping("/cooldown")
    public String updateCooldown(@RequestParam("cooldownDays") int cooldownDays,
                                 Principal principal,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (cooldownDays < 1 || cooldownDays > 365) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cooldown days must be between 1 and 365.");
            return "redirect:/admin/settings";
        }

        String key = "donor.cooldown.days";
        Setting setting = settingRepository.findBySettingKey(key)
                .orElse(new Setting(key, "90", "Required interval in days between whole-blood donations"));

        String oldVal = setting.getSettingValue();
        setting.setSettingValue(String.valueOf(cooldownDays));
        settingRepository.save(setting);

        // Record in Audit Trail
        User actor = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(AuditAction.UPDATE);
        log.setEntityName("Setting");
        log.setEntityId(key);
        log.setOldValue(oldVal);
        log.setNewValue(String.valueOf(cooldownDays));
        log.setDescription("Administrator updated donor cooldown interval from " + oldVal + " to " + cooldownDays + " days.");
        log.setIpAddress(request.getRemoteAddr());
        auditLogRepository.save(log);

        redirectAttributes.addFlashAttribute("successMessage", "Donor donation cooldown interval updated to " + cooldownDays + " days.");
        return "redirect:/admin/settings";
    }
}
