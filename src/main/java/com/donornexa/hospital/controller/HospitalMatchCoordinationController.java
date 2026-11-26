package com.donornexa.hospital.controller;

import com.donornexa.donation.entity.Donation;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalDonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Controller managing the operational donor assignment lifecycle:
 * scheduling donation appointments, logging donor arrival, marking donation in progress,
 * and hospital confirmation of actual collected units.
 */
@Slf4j
@Controller
@RequestMapping("/hospital/matches")
@PreAuthorize("hasRole('HOSPITAL')")
@RequiredArgsConstructor
public class HospitalMatchCoordinationController {

    private final HospitalDonationService hospitalDonationService;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;
    private final com.donornexa.audit.repository.AuditLogRepository auditLogRepository;
    private final com.donornexa.user.repository.UserRepository userRepository;

    @PostMapping("/{id}/schedule")
    public String scheduleAppointment(
            @PathVariable("id") Long matchId,
            @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime,
            @RequestParam(value = "appointmentNotes", required = false) String appointmentNotes,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            RequestMatch scheduled = hospitalDonationService.scheduleMatchAppointment(
                    email, matchId, appointmentTime, appointmentNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Appointment scheduled with donor " + scheduled.getDonor().getUser().getFullName() + " for "
                            + scheduled.getScheduledAppointmentTime().toString().replace('T', ' ') + ".");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to schedule appointment for match #{}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to schedule appointment: " + ex.getMessage());
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/hospital/dashboard";
    }

    @PostMapping("/{id}/mark-arrived")
    public String markArrived(
            @PathVariable("id") Long matchId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            RequestMatch match = hospitalDonationService.markMatchArrived(email, matchId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donor " + match.getDonor().getUser().getFullName() + " marked as ARRIVED at hospital.");
        } catch (Exception ex) {
            log.error("Failed to mark donor arrived for match #{}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/hospital/dashboard";
    }

    @PostMapping("/{id}/mark-in-progress")
    public String markInProgress(
            @PathVariable("id") Long matchId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            RequestMatch match = hospitalDonationService.markMatchInProgress(email, matchId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donation session for donor " + match.getDonor().getUser().getFullName() + " is now IN PROGRESS.");
        } catch (Exception ex) {
            log.error("Failed to mark donation in progress for match #{}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/hospital/dashboard";
    }

    @PostMapping("/{id}/mark-no-show")
    public String markNoShow(
            @PathVariable("id") Long matchId,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            RequestMatch match = hospitalDonationService.markMatchNoShow(email, matchId, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donor " + match.getDonor().getUser().getFullName() + " marked as NO SHOW.");
        } catch (Exception ex) {
            log.error("Failed to mark no show for match #{}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + ex.getMessage());
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/hospital/dashboard";
    }

    @PostMapping("/{id}/confirm")
    public String confirmDonation(
            @PathVariable("id") Long matchId,
            @RequestParam(value = "unitsDonated", defaultValue = "1") Integer unitsDonated,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "addToInventory", defaultValue = "true") boolean addToInventory,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            Donation donation = hospitalDonationService.confirmMatchDonation(
                    email, matchId, unitsDonated, notes, addToInventory);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donation confirmed successfully! Recorded " + donation.getUnitsDonated()
                            + " unit(s) of " + donation.getDonor().getBloodGroup().name().replace('_', ' ')
                            + " blood. Hospital inventory and request fulfillment updated.");
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to confirm donation for match #{}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Confirmation failed: " + ex.getMessage());
        }

        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/hospital/dashboard";
    }

    @PostMapping(value = "/{id}/reveal", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> revealDonorContact(
            @PathVariable("id") Long matchId,
            @AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String email = userDetails.getUsername();
        RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Match record not found with ID: " + matchId));

        if (!match.getBloodRequest().getHospital().getUser().getEmail().equalsIgnoreCase(email.trim())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own the blood request linked to this match.");
        }

        String rawPhone = match.getDonor().getUser() != null ? match.getDonor().getUser().getPhoneNumber() : "";
        String donorName = match.getDonor().getUser() != null ? match.getDonor().getUser().getFullName() : "Donor #" + match.getDonor().getId();
        String hospitalName = match.getBloodRequest().getHospital().getHospitalName();

        // Audit REVEAL_CONTACT event
        try {
            com.donornexa.audit.entity.AuditLog logEntry = new com.donornexa.audit.entity.AuditLog();
            userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(logEntry::setActor);
            logEntry.setAction(com.donornexa.common.enums.AuditAction.REVEAL_CONTACT);
            logEntry.setEntityName("Donor");
            logEntry.setEntityId(String.valueOf(match.getDonor().getId()));
            logEntry.setDescription(String.format("Hospital %s revealed contact phone number for %s regarding Blood Request #%d",
                    hospitalName, donorName, match.getBloodRequest().getId()));
            String clientIp = httpRequest.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = httpRequest.getRemoteAddr();
            }
            logEntry.setIpAddress(clientIp);
            logEntry.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to save REVEAL_CONTACT audit log: {}", ex.getMessage());
        }

        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "phoneNumber", rawPhone,
                "donorName", donorName,
                "matchId", matchId
        ));
    }
}
