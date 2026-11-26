package com.donornexa.hospital.controller;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.hospital.dto.RecordDonationDTO;
import com.donornexa.hospital.dto.UpdateDonationStatusDTO;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalDonationService;
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
@RequestMapping("/hospital/donations")
@PreAuthorize("hasRole('HOSPITAL')")
@RequiredArgsConstructor
public class HospitalDonationController {

    private final HospitalDonationService hospitalDonationService;

    @GetMapping
    public String listDonations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "status", required = false) DonationStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)), Sort.by("donationDate").descending());
        Page<Donation> donationPage = hospitalDonationService.getHospitalDonations(email, query, status, pageable);

        model.addAttribute("donationPage", donationPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", DonationStatus.values());
        model.addAttribute("userEmail", email);
        return "hospital/donations";
    }

    @GetMapping("/record")
    public String showRecordDonationForm(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "donorId", required = false) Long donorId,
            @RequestParam(value = "requestId", required = false) Long requestId,
            Model model) {

        String email = userDetails.getUsername();
        Hospital hospital = hospitalDonationService.getHospitalByEmail(email);

        RecordDonationDTO form = new RecordDonationDTO();
        if (donorId != null) form.setDonorId(donorId);
        if (requestId != null) form.setBloodRequestId(requestId);

        List<Donor> verifiedDonors = hospitalDonationService.getVerifiedDonors();
        List<BloodRequest> activeRequests = hospitalDonationService.getActiveRequestsForHospital(hospital.getId());

        model.addAttribute("donationForm", form);
        model.addAttribute("verifiedDonors", verifiedDonors);
        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("statuses", List.of(DonationStatus.COMPLETED, DonationStatus.SCHEDULED));
        model.addAttribute("userEmail", email);
        return "hospital/record-donation";
    }

    @PostMapping("/record")
    public String submitRecordDonation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("donationForm") RecordDonationDTO donationForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Hospital hospital = hospitalDonationService.getHospitalByEmail(email);
            model.addAttribute("verifiedDonors", hospitalDonationService.getVerifiedDonors());
            model.addAttribute("activeRequests", hospitalDonationService.getActiveRequestsForHospital(hospital.getId()));
            model.addAttribute("statuses", List.of(DonationStatus.COMPLETED, DonationStatus.SCHEDULED));
            model.addAttribute("userEmail", email);
            return "hospital/record-donation";
        }

        try {
            Donation donation = hospitalDonationService.recordDonation(email, donationForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Donation #" + donation.getId() + " recorded successfully." +
                    (donation.getInventoryAdded() ? " Blood units were added to the hospital blood bank inventory." : ""));
            return "redirect:/hospital/donations/" + donation.getId();
        } catch (HospitalOperationException ex) {
            Hospital hospital = hospitalDonationService.getHospitalByEmail(email);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("verifiedDonors", hospitalDonationService.getVerifiedDonors());
            model.addAttribute("activeRequests", hospitalDonationService.getActiveRequestsForHospital(hospital.getId()));
            model.addAttribute("statuses", List.of(DonationStatus.COMPLETED, DonationStatus.SCHEDULED));
            model.addAttribute("userEmail", email);
            return "hospital/record-donation";
        }
    }

    @GetMapping("/{id}")
    public String viewDonationDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            Donation donation = hospitalDonationService.getDonationByIdForHospital(email, id);
            UpdateDonationStatusDTO statusForm = new UpdateDonationStatusDTO();
            statusForm.setStatus(donation.getDonationStatus());

            model.addAttribute("donation", donation);
            model.addAttribute("statusForm", statusForm);
            model.addAttribute("statuses", DonationStatus.values());
            model.addAttribute("userEmail", email);
            return "hospital/donation-details";
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hospital/donations";
        }
    }

    @PostMapping("/{id}/status")
    public String updateDonationStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("statusForm") UpdateDonationStatusDTO statusForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid status selection.");
            return "redirect:/hospital/donations/" + id;
        }

        try {
            hospitalDonationService.updateDonationStatus(email, id, statusForm);
            redirectAttributes.addFlashAttribute("successMessage", "Donation status updated successfully to: " + statusForm.getStatus());
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital/donations/" + id;
    }

    @PostMapping("/{id}/add-to-inventory")
    public String addToInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        try {
            hospitalDonationService.addDonationToInventory(email, id);
            redirectAttributes.addFlashAttribute("successMessage", "Donation units added to blood bank inventory successfully.");
        } catch (HospitalOperationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital/donations/" + id;
    }
}
