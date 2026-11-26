package com.donornexa.admin.controller;

import com.donornexa.admin.dto.AdminPasswordChangeDTO;
import com.donornexa.admin.dto.AdminProfileUpdateDTO;
import com.donornexa.admin.service.AdminProfileService;
import com.donornexa.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller allowing administrators to inspect and update their profile details
 * and change credentials securely.
 */
@Slf4j
@Controller
@RequestMapping("/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User admin = adminProfileService.getAdminUser(userDetails.getUsername());
        model.addAttribute("adminUser", admin);

        if (!model.containsAttribute("profileDTO")) {
            AdminProfileUpdateDTO profileDTO = new AdminProfileUpdateDTO();
            profileDTO.setFullName(admin.getFullName());
            profileDTO.setPhoneNumber(admin.getPhoneNumber());
            model.addAttribute("profileDTO", profileDTO);
        }

        if (!model.containsAttribute("passwordDTO")) {
            model.addAttribute("passwordDTO", new AdminPasswordChangeDTO());
        }

        return "admin/profile";
    }

    @PostMapping("/edit")
    public String updateProfile(@Valid @ModelAttribute("profileDTO") AdminProfileUpdateDTO dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileDTO", bindingResult);
            redirectAttributes.addFlashAttribute("profileDTO", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors in profile submission.");
            return "redirect:/admin/profile";
        }

        try {
            adminProfileService.updateProfile(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details successfully updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to update admin profile {}: {}", userDetails.getUsername(), ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update profile: " + ex.getMessage());
        }

        return "redirect:/admin/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordDTO") AdminPasswordChangeDTO dto,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordDTO", bindingResult);
            redirectAttributes.addFlashAttribute("passwordDTO", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid password change parameters.");
            return "redirect:/admin/profile";
        }

        try {
            adminProfileService.changePassword(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to change password for {}: {}", userDetails.getUsername(), ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Password update failed: " + ex.getMessage());
        }

        return "redirect:/admin/profile";
    }
}
