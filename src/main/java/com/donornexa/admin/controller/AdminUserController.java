package com.donornexa.admin.controller;

import com.donornexa.admin.dto.AdminCreateUserDTO;
import com.donornexa.admin.dto.AdminUserStatusDTO;
import com.donornexa.admin.service.AdminUserService;
import com.donornexa.common.enums.Role;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

/**
 * Controller managing administrative user search, directory browsing, inspection,
 * and account status lifecycle transitions.
 */
@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;

    @GetMapping
    public String listUsers(@RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "role", required = false) Role role,
                            @RequestParam(value = "accountStatus", required = false) AccountStatus accountStatus,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("createdAt").descending());

        Page<User> userPage = adminUserService.searchUsers(query, role, accountStatus, pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedAccountStatus", accountStatus);

        model.addAttribute("roles", Role.values());
        model.addAttribute("accountStatuses", AccountStatus.values());

        if (!model.containsAttribute("createAdminDTO")) {
            model.addAttribute("createAdminDTO", new AdminCreateUserDTO());
        }

        return "admin/users";
    }

    @GetMapping("/{id}")
    public String viewUserDetails(@PathVariable("id") Long id,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = adminUserService.getUserById(id);
            model.addAttribute("inspectedUser", user);

            // Related profile lookups if donor or hospital
            if (user.getRole() == Role.DONOR) {
                donorRepository.findByUserId(user.getId()).ifPresent(d -> model.addAttribute("donorProfile", d));
            } else if (user.getRole() == Role.HOSPITAL) {
                hospitalRepository.findByUserId(user.getId()).ifPresent(h -> model.addAttribute("hospitalProfile", h));
            }

            if (!model.containsAttribute("statusDTO")) {
                AdminUserStatusDTO statusDTO = new AdminUserStatusDTO();
                statusDTO.setAccountStatus(user.getAccountStatus());
                model.addAttribute("statusDTO", statusDTO);
            }

            model.addAttribute("accountStatuses", AccountStatus.values());
            return "admin/user-details";

        } catch (NoSuchElementException ex) {
            log.error("User lookup failed for ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/create-admin")
    public String createAdministrator(@Valid @ModelAttribute("createAdminDTO") AdminCreateUserDTO createAdminDTO,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/users";
        }

        try {
            adminUserService.createAdministrator(createAdminDTO, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "New administrator account created successfully for " + createAdminDTO.getEmail());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to create administrator account", ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create administrator account: " + ex.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("statusDTO") AdminUserStatusDTO dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status submission.");
            return "redirect:/admin/users/" + id;
        }

        try {
            User updated = adminUserService.updateUserStatus(id, dto.getAccountStatus(), userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "User '" + updated.getEmail() + "' status successfully changed to " + updated.getAccountStatus() + ".");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to update status for user {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred while updating account status: " + ex.getMessage());
        }

        return "redirect:/admin/users/" + id;
    }
}
