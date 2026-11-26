package com.donornexa.admin.controller;

import com.donornexa.admin.dto.AdminBloodRequestStatusDTO;
import com.donornexa.admin.service.AdminBloodRequestService;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
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
 * Controller for administrative monitoring, filtering, and status management of blood requests.
 */
@Slf4j
@Controller
@RequestMapping("/admin/blood-requests")
@RequiredArgsConstructor
public class AdminBloodRequestController {

    private final AdminBloodRequestService adminBloodRequestService;

    @GetMapping
    public String listRequests(@RequestParam(value = "query", required = false) String query,
                               @RequestParam(value = "status", required = false) RequestStatus status,
                               @RequestParam(value = "urgency", required = false) RequestUrgency urgency,
                               @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("createdAt").descending());

        Page<BloodRequest> requestPage = adminBloodRequestService.searchBloodRequests(query, status, urgency, bloodGroup, pageable);

        model.addAttribute("requestPage", requestPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUrgency", urgency);
        model.addAttribute("selectedBloodGroup", bloodGroup);

        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("urgencies", RequestUrgency.values());
        model.addAttribute("bloodGroups", BloodGroup.values());

        return "admin/blood-requests";
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Long id,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            BloodRequest bloodRequest = adminBloodRequestService.getBloodRequestById(id);
            model.addAttribute("bloodRequest", bloodRequest);

            if (!model.containsAttribute("statusDTO")) {
                AdminBloodRequestStatusDTO statusDTO = new AdminBloodRequestStatusDTO();
                statusDTO.setStatus(bloodRequest.getStatus());
                model.addAttribute("statusDTO", statusDTO);
            }

            model.addAttribute("statuses", RequestStatus.values());
            return "admin/blood-request-details";

        } catch (NoSuchElementException ex) {
            log.error("Blood request lookup failed for ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/blood-requests";
        }
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("statusDTO") AdminBloodRequestStatusDTO dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status selection.");
            return "redirect:/admin/blood-requests/" + id;
        }

        try {
            BloodRequest updated = adminBloodRequestService.updateStatus(id, dto.getStatus(), userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Blood request #" + id + " status successfully updated to " + updated.getStatus() + ".");
        } catch (Exception ex) {
            log.error("Failed to update blood request status {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update blood request: " + ex.getMessage());
        }

        return "redirect:/admin/blood-requests/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable("id") Long id,
                                 @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            BloodRequest approved = adminBloodRequestService.approveBloodRequest(id, adminNotes, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Blood request #" + id + " has been APPROVED. Hospital coordinator has been notified.");
        } catch (Exception ex) {
            log.error("Failed to approve blood request #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to approve request: " + ex.getMessage());
        }
        return "redirect:/admin/blood-requests/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable("id") Long id,
                                @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            BloodRequest rejected = adminBloodRequestService.rejectBloodRequest(id, rejectionReason, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Blood request #" + id + " has been REJECTED. Hospital coordinator has been notified.");
        } catch (Exception ex) {
            log.error("Failed to reject blood request #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject request: " + ex.getMessage());
        }
        return "redirect:/admin/blood-requests/" + id;
    }
}
