package com.donornexa.admin.controller;

import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.hospital.dto.HospitalAccountStatusActionDTO;
import com.donornexa.hospital.dto.HospitalProfileDTO;
import com.donornexa.hospital.dto.HospitalVerificationActionDTO;
import com.donornexa.hospital.dto.HospitalVerificationStatus;
import com.donornexa.common.exception.HospitalNotFoundException;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalService;
import com.donornexa.hospital.service.HospitalVerificationService;
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
 * and account status management for hospitals.
 */
@Slf4j
@Controller
@RequestMapping("/admin/hospitals")
@RequiredArgsConstructor
public class AdminHospitalController {

    private final HospitalService hospitalService;
    private final HospitalVerificationService verificationService;

    /**
     * View hospital listing with keyword search, status filtering, and pagination.
     */
    @GetMapping
    public String listHospitals(@RequestParam(value = "query", required = false) String query,
                                @RequestParam(value = "verificationStatus", required = false) HospitalVerificationStatus verificationStatus,
                                @RequestParam(value = "accountStatus", required = false) AccountStatus accountStatus,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("createdAt").descending());

        Page<Hospital> hospitalPage = verificationService.searchHospitals(query, verificationStatus, accountStatus, pageable);

        model.addAttribute("hospitalPage", hospitalPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedVerificationStatus", verificationStatus);
        model.addAttribute("selectedAccountStatus", accountStatus);
        model.addAttribute("verificationStatuses", HospitalVerificationStatus.values());
        model.addAttribute("accountStatuses", AccountStatus.values());

        // Quick metric badges
        model.addAttribute("totalHospitals", verificationService.getTotalHospitalsCount());
        model.addAttribute("pendingCount", verificationService.getPendingVerificationCount());
        model.addAttribute("verifiedCount", verificationService.getVerifiedHospitalsCount());
        model.addAttribute("rejectedCount", verificationService.getRejectedHospitalsCount());
        model.addAttribute("suspendedCount", verificationService.getSuspendedHospitalsCount());

        model.addAttribute("userEmail", userDetails.getUsername());
        return "admin/hospitals";
    }

    /**
     * Detailed inspection view for a single hospital.
     */
    @GetMapping("/{id}")
    public String inspectHospital(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        try {
            HospitalProfileDTO hospital = hospitalService.getHospitalProfileById(id);
            model.addAttribute("hospital", hospital);

            HospitalVerificationActionDTO verificationForm = new HospitalVerificationActionDTO();
            model.addAttribute("verificationForm", verificationForm);

            HospitalAccountStatusActionDTO statusForm = new HospitalAccountStatusActionDTO();
            statusForm.setAccountStatus(hospital.getAccountStatus());
            model.addAttribute("statusForm", statusForm);

            model.addAttribute("accountStatuses", AccountStatus.values());
            model.addAttribute("userEmail", userDetails.getUsername());
            return "admin/hospital-details";
        } catch (HospitalNotFoundException ex) {
            return "redirect:/admin/hospitals";
        }
    }

    /**
     * Process verification decision (APPROVE or REJECT) for a hospital.
     */
    @PostMapping("/{id}/verify")
    public String processVerification(@PathVariable("id") Long id,
                                      @Valid @ModelAttribute("verificationForm") HospitalVerificationActionDTO verificationForm,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/hospitals/" + id;
        }

        try {
            verificationService.verifyHospital(id, verificationForm, userDetails.getUsername());
            String actionVerb = "APPROVE".equalsIgnoreCase(verificationForm.getAction()) ? "approved" : "rejected";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hospital verification successfully " + actionVerb + ".");
        } catch (HospitalOperationException | HospitalNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/hospitals/" + id;
    }

    /**
     * Update account lifecycle status (ACTIVE, SUSPENDED, INACTIVE) for a hospital.
     */
    @PostMapping("/{id}/status")
    public String updateAccountStatus(@PathVariable("id") Long id,
                                      @Valid @ModelAttribute("statusForm") HospitalAccountStatusActionDTO statusForm,
                                      BindingResult bindingResult,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/hospitals/" + id;
        }

        try {
            verificationService.updateAccountStatus(id, statusForm, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hospital account status updated to " + statusForm.getAccountStatus().name() + ".");
        } catch (HospitalOperationException | HospitalNotFoundException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/hospitals/" + id;
    }
}
