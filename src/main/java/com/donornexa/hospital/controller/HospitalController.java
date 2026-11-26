package com.donornexa.hospital.controller;

import com.donornexa.hospital.entity.Hospital;
import com.donornexa.hospital.dto.HospitalDashboardSummary;
import com.donornexa.hospital.dto.HospitalProfileDTO;
import com.donornexa.hospital.dto.HospitalProfileUpdateDTO;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalService;
import com.donornexa.location.repository.ProvinceRepository;
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
 * Controller managing hospital portal pages: dashboard, profile view,
 * profile editing with dependent location dropdowns, and verification status.
 */
@Slf4j
@Controller
@RequestMapping("/hospital")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;
    private final ProvinceRepository provinceRepository;

    /**
     * Hospital dashboard displaying live database statistics and profile status.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        HospitalDashboardSummary summary = hospitalService.getDashboardSummary(email);

        model.addAttribute("summary", summary);
        model.addAttribute("userEmail", email);
        return "hospital/dashboard";
    }

    /**
     * View hospital profile.
     */
    @GetMapping("/profile")
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        HospitalProfileDTO profile = hospitalService.getHospitalProfile(email);

        model.addAttribute("profile", profile);
        model.addAttribute("userEmail", email);
        return "hospital/profile";
    }

    /**
     * Display edit profile form.
     */
    @GetMapping("/profile/edit")
    public String editProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Hospital hospital = hospitalService.getHospitalByEmail(email);

        HospitalProfileUpdateDTO form = new HospitalProfileUpdateDTO();
        form.setHospitalName(hospital.getHospitalName());
        form.setContactNumber(hospital.getContactNumber());
        form.setProvinceId(hospital.getProvince().getId());
        form.setCityId(hospital.getCity().getId());
        form.setCompleteAddress(hospital.getCompleteAddress());

        model.addAttribute("profileForm", form);
        model.addAttribute("provinces", provinceRepository.findAll());
        model.addAttribute("hospital", hospital);
        model.addAttribute("userEmail", email);
        return "hospital/edit-profile";
    }

    /**
     * Process hospital profile update.
     */
    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileForm") HospitalProfileUpdateDTO profileForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Hospital hospital = hospitalService.getHospitalByEmail(email);
            model.addAttribute("hospital", hospital);
            model.addAttribute("provinces", provinceRepository.findAll());
            model.addAttribute("userEmail", email);
            return "hospital/edit-profile";
        }

        try {
            hospitalService.updateHospitalProfile(email, profileForm);
            redirectAttributes.addFlashAttribute("successMessage", "Hospital profile updated successfully.");
            return "redirect:/hospital/profile";
        } catch (HospitalOperationException ex) {
            Hospital hospital = hospitalService.getHospitalByEmail(email);
            model.addAttribute("hospital", hospital);
            model.addAttribute("provinces", provinceRepository.findAll());
            model.addAttribute("userEmail", email);
            model.addAttribute("errorMessage", ex.getMessage());
            return "hospital/edit-profile";
        }
    }

    /**
     * View verification status page.
     */
    @GetMapping("/verification")
    public String verificationStatus(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        HospitalProfileDTO profile = hospitalService.getHospitalProfile(email);

        model.addAttribute("profile", profile);
        model.addAttribute("userEmail", email);
        return "hospital/verification-status";
    }
}
