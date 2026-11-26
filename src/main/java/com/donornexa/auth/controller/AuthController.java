package com.donornexa.auth.controller;

import com.donornexa.auth.dto.DonorRegistrationRequest;
import com.donornexa.auth.dto.HospitalRegistrationRequest;
import com.donornexa.common.exception.RegistrationException;
import com.donornexa.auth.service.AuthService;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.Gender;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles all authentication-related HTTP requests:
 * registration selection, donor registration, hospital registration.
 *
 * <p>Login and logout are handled directly by Spring Security â€” no controller
 * method is needed for those flows.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService        authService;
    private final ProvinceRepository provinceRepository;
    private final CityRepository     cityRepository;

    // =========================================================================
    // Registration type selector
    // =========================================================================

    @GetMapping("/register")
    public String registerSelector() {
        return "auth/register";
    }

    // =========================================================================
    // Donor registration
    // =========================================================================

    @GetMapping({"/register/donor", "/donor-register"})
    public String showDonorRegistration(Model model) {
        model.addAttribute("donorForm",   new DonorRegistrationRequest());
        model.addAttribute("provinces",   provinceRepository.findAllByOrderByNameAsc());
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("genders",     Gender.values());
        model.addAttribute("cities",      List.of());   // populated client-side
        return "auth/donor-register";
    }

    @PostMapping({"/register/donor", "/donor-register"})
    public String submitDonorRegistration(
            @Valid @ModelAttribute("donorForm") DonorRegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("provinces",   provinceRepository.findAllByOrderByNameAsc());
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("genders",     Gender.values());
            // Reload cities for the previously selected province
            if (request.getProvinceId() != null) {
                model.addAttribute("cities",
                        cityRepository.findByProvinceIdOrderByNameAsc(request.getProvinceId()));
            } else {
                model.addAttribute("cities", List.of());
            }
            return "auth/donor-register";
        }

        try {
            authService.registerDonor(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! Welcome to DonorNexa. "
                    + "Please log in to access your dashboard.");
            return "redirect:/login";
        } catch (RegistrationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("provinces",   provinceRepository.findAllByOrderByNameAsc());
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("genders",     Gender.values());
            if (request.getProvinceId() != null) {
                model.addAttribute("cities",
                        cityRepository.findByProvinceIdOrderByNameAsc(request.getProvinceId()));
            } else {
                model.addAttribute("cities", List.of());
            }
            return "auth/donor-register";
        }
    }

    // =========================================================================
    // Hospital registration
    // =========================================================================

    @GetMapping({"/register/hospital", "/hospital-register"})
    public String showHospitalRegistration(Model model) {
        model.addAttribute("hospitalForm", new HospitalRegistrationRequest());
        model.addAttribute("provinces",    provinceRepository.findAllByOrderByNameAsc());
        model.addAttribute("cities",       List.of());
        return "auth/hospital-register";
    }

    @PostMapping({"/register/hospital", "/hospital-register"})
    public String submitHospitalRegistration(
            @Valid @ModelAttribute("hospitalForm") HospitalRegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
            if (request.getProvinceId() != null) {
                model.addAttribute("cities",
                        cityRepository.findByProvinceIdOrderByNameAsc(request.getProvinceId()));
            } else {
                model.addAttribute("cities", List.of());
            }
            return "auth/hospital-register";
        }

        try {
            authService.registerHospital(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Hospital registered successfully! "
                    + "Your account is pending verification by an administrator. "
                    + "You can log in now.");
            return "redirect:/login";
        } catch (RegistrationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
            if (request.getProvinceId() != null) {
                model.addAttribute("cities",
                        cityRepository.findByProvinceIdOrderByNameAsc(request.getProvinceId()));
            } else {
                model.addAttribute("cities", List.of());
            }
            return "auth/hospital-register";
        }
    }

    // =========================================================================
    // AJAX: cities by province (for backward compatibility)
    // =========================================================================

    @GetMapping(value = "/api/cities", params = "provinceId")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<CityDto> getCitiesByProvince(@RequestParam Long provinceId) {
        return cityRepository.findByProvinceIdOrderByNameAsc(provinceId)
                .stream()
                .map(c -> new CityDto(c.getId(), c.getName()))
                .toList();
    }

    /** Minimal projection used for the AJAX city endpoint. */
    public record CityDto(Long id, String name) {}
}
