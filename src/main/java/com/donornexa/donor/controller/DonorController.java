package com.donornexa.donor.controller;

import com.donornexa.donor.dto.DonorAvailabilityUpdateRequest;
import com.donornexa.donor.dto.DonorDashboardSummary;
import com.donornexa.donor.dto.DonorEligibilityResult;
import com.donornexa.donor.dto.DonorProfileUpdateRequest;
import com.donornexa.common.exception.DonorProfileException;
import com.donornexa.donor.service.DonorEligibilityService;
import com.donornexa.donor.service.DonorService;
import com.donornexa.location.entity.City;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.Province;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller managing the donor portal:
 * dashboard, profile viewing and editing, availability toggling,
 * eligibility criteria inspection, and donation history viewing.
 *
 * <p>All operations derive the target donor strictly from the authenticated
 * session principal (email) to prevent unauthorized cross-account access.
 */
@Slf4j
@Controller
@RequestMapping("/donor")
@RequiredArgsConstructor
public class DonorController {

    private final DonorService donorService;
    private final DonorEligibilityService eligibilityService;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final com.donornexa.donation.repository.DonationRepository donationRepository;
    private final com.donornexa.badge.service.DonationBadgeService donationBadgeService;

    // =========================================================================
    // 1. Dashboard
    // =========================================================================

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        DonorDashboardSummary summary = donorService.getDashboardSummary(email);

        model.addAttribute("summary", summary);
        model.addAttribute("donor", summary.getDonor());
        model.addAttribute("eligibility", summary.getEligibility());
        model.addAttribute("recentDonations", summary.getRecentDonations());
        model.addAttribute("badgeProfile", donationBadgeService.getBadgeProfileForDonor(summary.getDonor().getId()));
        model.addAttribute("userEmail", email);
        return "donor/dashboard";
    }

    // =========================================================================
    // 2. Profile Details (Read-only view)
    // =========================================================================

    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Donor donor = donorService.getDonorByUserEmail(email);
        DonorEligibilityResult eligibility = eligibilityService.evaluateEligibility(donor);

        model.addAttribute("donor", donor);
        model.addAttribute("eligibility", eligibility);
        model.addAttribute("userEmail", email);
        return "donor/profile";
    }

    // =========================================================================
    // 3. Edit Profile (Permitted personal & location fields)
    // =========================================================================

    @GetMapping("/profile/edit")
    public String showEditProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Donor donor = donorService.getDonorByUserEmail(email);
        DonorProfileUpdateRequest profileForm = donorService.getProfileUpdateRequest(email);

        List<Province> provinces = provinceRepository.findAllByOrderByNameAsc();
        List<City> cities = cityRepository.findByProvinceIdOrderByNameAsc(donor.getProvince().getId());

        model.addAttribute("donor", donor);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("provinces", provinces);
        model.addAttribute("cities", cities);
        model.addAttribute("userEmail", email);
        return "donor/edit-profile";
    }

    @PostMapping("/profile/edit")
    public String submitEditProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("profileForm") DonorProfileUpdateRequest profileForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Donor donor = donorService.getDonorByUserEmail(email);
            populateLocationModel(model, profileForm.getProvinceId(), donor.getProvince().getId());
            model.addAttribute("donor", donor);
            model.addAttribute("userEmail", email);
            return "donor/edit-profile";
        }

        try {
            donorService.updateProfile(email, profileForm);
            redirectAttributes.addFlashAttribute("successMessage", "Your profile has been updated successfully.");
            return "redirect:/donor/profile";
        } catch (DonorProfileException ex) {
            Donor donor = donorService.getDonorByUserEmail(email);
            populateLocationModel(model, profileForm.getProvinceId(), donor.getProvince().getId());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("donor", donor);
            model.addAttribute("userEmail", email);
            return "donor/edit-profile";
        }
    }

    // =========================================================================
    // 4. Availability Management
    // =========================================================================

    @GetMapping("/availability")
    public String viewAvailability(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Donor donor = donorService.getDonorByUserEmail(email);
        DonorEligibilityResult eligibility = eligibilityService.evaluateEligibility(donor);

        DonorAvailabilityUpdateRequest availabilityForm = new DonorAvailabilityUpdateRequest();
        availabilityForm.setAvailability(donor.getAvailability());

        model.addAttribute("donor", donor);
        model.addAttribute("eligibility", eligibility);
        model.addAttribute("availabilityForm", availabilityForm);
        model.addAttribute("availabilities", DonorAvailability.values());
        model.addAttribute("userEmail", email);
        return "donor/availability";
    }

    @PostMapping("/availability")
    public String updateAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("availabilityForm") DonorAvailabilityUpdateRequest availabilityForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Donor donor = donorService.getDonorByUserEmail(email);
            DonorEligibilityResult eligibility = eligibilityService.evaluateEligibility(donor);
            model.addAttribute("donor", donor);
            model.addAttribute("eligibility", eligibility);
            model.addAttribute("availabilities", DonorAvailability.values());
            model.addAttribute("userEmail", email);
            return "donor/availability";
        }

        try {
            donorService.updateAvailability(email, availabilityForm.getAvailability());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Availability status successfully changed to: " +
                            availabilityForm.getAvailability().name().replace('_', ' '));
            return "redirect:/donor/availability";
        } catch (DonorProfileException ex) {
            Donor donor = donorService.getDonorByUserEmail(email);
            DonorEligibilityResult eligibility = eligibilityService.evaluateEligibility(donor);
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("donor", donor);
            model.addAttribute("eligibility", eligibility);
            model.addAttribute("availabilities", DonorAvailability.values());
            model.addAttribute("userEmail", email);
            return "donor/availability";
        }
    }

    // =========================================================================
    // 5. Donation History
    // =========================================================================

    @GetMapping("/donations")
    public String viewDonationHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Donor donor = donorService.getDonorByUserEmail(email);
        List<Donation> donations = donorService.getDonationHistory(email);

        model.addAttribute("donor", donor);
        model.addAttribute("donations", donations);
        model.addAttribute("userEmail", email);
        return "donor/donation-history";
    }

    @GetMapping("/donations/{id}")
    public String viewDonationDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        String email = userDetails.getUsername();
        Donor donor = donorService.getDonorByUserEmail(email);

        Donation donation = donationRepository.findByIdWithDetails(id)
                .orElse(null);

        if (donation == null || !donation.getDonor().getId().equals(donor.getId())) {
            log.warn("IDOR attempt: Donor {} attempted to access donation ID {}", donor.getId(), id);
            redirectAttributes.addFlashAttribute("errorMessage", "Donation record not found or access denied.");
            return "redirect:/donor/donations";
        }

        model.addAttribute("donor", donor);
        model.addAttribute("donation", donation);
        model.addAttribute("userEmail", email);
        return "donor/donation-details";
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private void populateLocationModel(Model model, Long formProvinceId, Long fallbackProvinceId) {
        model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
        Long selectedProvinceId = formProvinceId != null ? formProvinceId : fallbackProvinceId;
        if (selectedProvinceId != null) {
            model.addAttribute("cities", cityRepository.findByProvinceIdOrderByNameAsc(selectedProvinceId));
        } else {
            model.addAttribute("cities", List.of());
        }
    }
}
