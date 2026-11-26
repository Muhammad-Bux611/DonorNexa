package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminDonorVerificationService;
import com.donornexa.donor.dto.DonorAccountStatusActionDTO;
import com.donornexa.donor.dto.DonorVerificationActionDTO;
import com.donornexa.donor.dto.DonorVerificationStatus;
import com.donornexa.common.exception.DonorProfileException;
import com.donornexa.donor.entity.Donor;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.BloodGroup;
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

/**
 * Controller managing administrative inspection, verification, search,
 * and account status management for donors.
 */
@Slf4j
@Controller
@RequestMapping("/admin/donors")
@RequiredArgsConstructor
public class AdminDonorController {

    private final AdminDonorVerificationService verificationService;

    /**
     * View donor listing with keyword search, status filtering, and pagination.
     */
    @GetMapping
    public String listDonors(@RequestParam(value = "query", required = false) String query,
                             @RequestParam(value = "verificationStatus", required = false) DonorVerificationStatus verificationStatus,
                             @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
                             @RequestParam(value = "accountStatus", required = false) AccountStatus accountStatus,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("createdAt").descending());

        Page<Donor> donorPage = verificationService.searchDonors(query, verificationStatus, bloodGroup, accountStatus, pageable);

        model.addAttribute("donorPage", donorPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedVerificationStatus", verificationStatus);
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("selectedAccountStatus", accountStatus);

        model.addAttribute("verificationStatuses", DonorVerificationStatus.values());
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("accountStatuses", AccountStatus.values());

        model.addAttribute("pendingCount", verificationService.getPendingVerificationCount());
        model.addAttribute("verifiedCount", verificationService.getVerifiedDonorsCount());
        model.addAttribute("totalCount", verificationService.getTotalDonorsCount());

        return "admin/donors";
    }

    /**
     * Detailed inspection view for a single donor profile.
     */
    @GetMapping("/{id}")
    public String viewDonorDetails(@PathVariable("id") Long id,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            Donor donor = verificationService.getDonorById(id);
            model.addAttribute("donor", donor);

            if (!model.containsAttribute("verificationActionDTO")) {
                model.addAttribute("verificationActionDTO", new DonorVerificationActionDTO());
            }

            if (!model.containsAttribute("accountStatusActionDTO")) {
                DonorAccountStatusActionDTO statusDTO = new DonorAccountStatusActionDTO();
                statusDTO.setAccountStatus(donor.getUser().getAccountStatus());
                model.addAttribute("accountStatusActionDTO", statusDTO);
            }

            model.addAttribute("accountStatuses", AccountStatus.values());

            return "admin/donor-details";

        } catch (DonorProfileException ex) {
            log.error("Donor details lookup failed for ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/donors";
        }
    }

    /**
     * Process verification decision (APPROVE or REJECT) for a donor.
     */
    @PostMapping("/{id}/verify")
    public String verifyDonor(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("verificationActionDTO") DonorVerificationActionDTO dto,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.verificationActionDTO", bindingResult);
            redirectAttributes.addFlashAttribute("verificationActionDTO", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid verification submission. Please review the form.");
            return "redirect:/admin/donors/" + id;
        }

        try {
            Donor updated = verificationService.verifyDonor(id, dto, userDetails.getUsername());
            String actionVerb = "APPROVE".equalsIgnoreCase(dto.getAction()) ? "approved" : "rejected";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donor '" + updated.getUser().getFullName() + "' verification successfully " + actionVerb + ".");
        } catch (DonorProfileException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error executing verification decision on donor {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred while processing the verification.");
        }

        return "redirect:/admin/donors/" + id;
    }

    /**
     * Transition a donor's account status (ACTIVE, SUSPENDED, INACTIVE).
     */
    @PostMapping("/{id}/status")
    public String updateAccountStatus(@PathVariable("id") Long id,
                                      @Valid @ModelAttribute("accountStatusActionDTO") DonorAccountStatusActionDTO dto,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid account status provided.");
            return "redirect:/admin/donors/" + id;
        }

        try {
            Donor updated = verificationService.updateAccountStatus(id, dto, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account status for donor '" + updated.getUser().getFullName()
                            + "' successfully updated to " + dto.getAccountStatus() + ".");
        } catch (Exception ex) {
            log.error("Error updating account status for donor {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update donor account status: " + ex.getMessage());
        }

        return "redirect:/admin/donors/" + id;
    }
}
