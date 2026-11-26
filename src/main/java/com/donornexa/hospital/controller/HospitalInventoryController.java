package com.donornexa.hospital.controller;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.inventory.entity.BloodStockEntry;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.hospital.dto.DirectStockEntryDTO;
import com.donornexa.hospital.dto.HospitalInventorySummaryDTO;
import com.donornexa.hospital.dto.IssueStockDTO;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalDonationService;
import com.donornexa.hospital.service.HospitalInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/hospital/inventory")
@PreAuthorize("hasRole('HOSPITAL')")
@RequiredArgsConstructor
public class HospitalInventoryController {

    private final HospitalInventoryService hospitalInventoryService;
    private final HospitalDonationService hospitalDonationService;

    @GetMapping
    public String viewInventory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        HospitalInventorySummaryDTO summary = hospitalInventoryService.getInventorySummary(email);

        model.addAttribute("summary", summary);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("userEmail", email);
        return "hospital/inventory";
    }

    @GetMapping("/add")
    public String showDirectIntakeForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        DirectStockEntryDTO form = new DirectStockEntryDTO();

        model.addAttribute("intakeForm", form);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("userEmail", email);
        return "hospital/add-inventory";
    }

    @PostMapping("/add")
    public String submitDirectIntake(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("intakeForm") DirectStockEntryDTO intakeForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("userEmail", email);
            return "hospital/add-inventory";
        }

        try {
            hospitalInventoryService.addDirectStock(email, intakeForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Stock entry registered successfully: " + intakeForm.getUnits() + " unit(s) of " +
                    intakeForm.getBloodGroup().name().replace('_', ' ') + " added to bank inventory.");
            return "redirect:/hospital/inventory";
        } catch (HospitalOperationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("userEmail", email);
            return "hospital/add-inventory";
        }
    }

    @GetMapping("/issue")
    public String showIssueStockForm(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "requestId", required = false) Long requestId,
            Model model) {

        String email = userDetails.getUsername();
        Hospital hospital = hospitalInventoryService.getHospitalByEmail(email);

        IssueStockDTO form = new IssueStockDTO();
        if (requestId != null) {
            form.setBloodRequestId(requestId);
        }

        List<BloodRequest> activeRequests = hospitalDonationService.getActiveRequestsForHospital(hospital.getId());

        model.addAttribute("issueForm", form);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("userEmail", email);
        return "hospital/issue-inventory";
    }

    @PostMapping("/issue")
    public String submitIssueStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("issueForm") IssueStockDTO issueForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Hospital hospital = hospitalInventoryService.getHospitalByEmail(email);
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("activeRequests", hospitalDonationService.getActiveRequestsForHospital(hospital.getId()));
            model.addAttribute("userEmail", email);
            return "hospital/issue-inventory";
        }

        try {
            hospitalInventoryService.issueStock(email, issueForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Issued " + issueForm.getUnits() + " unit(s) of " +
                    issueForm.getBloodGroup().name().replace('_', ' ') +
                    (issueForm.getBloodRequestId() != null ? " against Request #" + issueForm.getBloodRequestId() : " for hospital clinical use."));
            return "redirect:/hospital/inventory";
        } catch (HospitalOperationException ex) {
            Hospital hospital = hospitalInventoryService.getHospitalByEmail(email);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("activeRequests", hospitalDonationService.getActiveRequestsForHospital(hospital.getId()));
            model.addAttribute("userEmail", email);
            return "hospital/issue-inventory";
        }
    }

    @PostMapping("/discard-expired")
    public String discardExpiredStock(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            int count = hospitalInventoryService.discardExpiredStock(email);
            if (count > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Cleaned up and discarded " + count + " expired unit(s) from active inventory.");
            } else {
                redirectAttributes.addFlashAttribute("infoMessage", "No expired blood units found in active stock.");
            }
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital/inventory";
    }

    @GetMapping("/history")
    public String viewHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by("createdAt").descending());
        Page<BloodStockEntry> entriesPage = hospitalInventoryService.getStockHistory(email, pageable);

        model.addAttribute("entriesPage", entriesPage);
        model.addAttribute("userEmail", email);
        return "hospital/inventory-history";
    }
}
