package com.donornexa.hospital.controller;

import com.donornexa.bloodrequest.dto.CreateBloodRequestDTO;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestUrgency;
import com.donornexa.hospital.service.HospitalBloodRequestService;
import com.donornexa.hospital.service.HospitalService;
import com.donornexa.matching.dto.MatchingEvaluationSummaryDTO;
import com.donornexa.matching.service.DonorMatchingService;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.location.repository.ProvinceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

/**
 * Controller handling hospital blood request management and donor matching execution.
 */
@Slf4j
@Controller
@RequestMapping("/hospital/blood-requests")
@RequiredArgsConstructor
public class HospitalBloodRequestController {

    private final HospitalBloodRequestService bloodRequestService;
    private final DonorMatchingService matchingService;
    private final HospitalService hospitalService;
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;

    @GetMapping
    public String listRequests(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               Model model) {
        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)),
                Sort.by("createdAt").descending());

        Page<BloodRequest> requestPage = bloodRequestService.getRequestsForHospital(email, pageable);
        Hospital hospital = hospitalService.getHospitalByEmail(email);

        model.addAttribute("requestPage", requestPage);
        model.addAttribute("hospital", hospital);
        model.addAttribute("userEmail", email);
        return "hospital/blood-requests";
    }

    @GetMapping("/new")
    public String newRequestForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        Hospital hospital = hospitalService.getHospitalByEmail(email);

        CreateBloodRequestDTO form = new CreateBloodRequestDTO();
        if (hospital.getProvince() != null) {
            form.setProvinceId(hospital.getProvince().getId());
        }
        if (hospital.getCity() != null) {
            form.setCityId(hospital.getCity().getId());
        }
        form.setRequiredBy(LocalDateTime.now().plusDays(2).withMinute(0).withSecond(0));

        model.addAttribute("requestForm", form);
        model.addAttribute("hospital", hospital);
        model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
        if (hospital.getProvince() != null) {
            model.addAttribute("cities", cityRepository.findByProvinceIdOrderByNameAsc(hospital.getProvince().getId()));
        }
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("urgencies", RequestUrgency.values());
        model.addAttribute("userEmail", email);
        return "hospital/create-blood-request";
    }

    @PostMapping("/new")
    public String createRequest(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("requestForm") CreateBloodRequestDTO form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            Hospital hospital = hospitalService.getHospitalByEmail(email);
            model.addAttribute("hospital", hospital);
            model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
            if (form.getProvinceId() != null) {
                model.addAttribute("cities", cityRepository.findByProvinceIdOrderByNameAsc(form.getProvinceId()));
            }
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("urgencies", RequestUrgency.values());
            model.addAttribute("userEmail", email);
            return "hospital/create-blood-request";
        }

        try {
            BloodRequest created = bloodRequestService.createRequest(email, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Blood request #" + created.getId() + " raised successfully! It is pending administrator approval before donor matching.");
            return "redirect:/hospital/blood-requests/" + created.getId();
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hospital/blood-requests";
        } catch (Exception ex) {
            log.error("Failed to create blood request: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            Hospital hospital = hospitalService.getHospitalByEmail(email);
            model.addAttribute("hospital", hospital);
            model.addAttribute("provinces", provinceRepository.findAllByOrderByNameAsc());
            model.addAttribute("bloodGroups", BloodGroup.values());
            model.addAttribute("urgencies", RequestUrgency.values());
            return "hospital/create-blood-request";
        }
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {
        String email = userDetails.getUsername();
        BloodRequest bloodRequest = bloodRequestService.getRequestByIdForHospital(id, email);

        model.addAttribute("bloodRequest", bloodRequest);
        model.addAttribute("userEmail", email);
        return "hospital/blood-request-details";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable("id") Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            bloodRequestService.cancelRequest(id, email);
            redirectAttributes.addFlashAttribute("successMessage", "Blood request #" + id + " has been cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hospital/blood-requests";
    }

    /**
     * View existing matching results for a blood request.
     */
    @GetMapping("/{id}/matches")
    public String viewMatches(@PathVariable("id") Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        String email = userDetails.getUsername();
        BloodRequest bloodRequest = bloodRequestService.getRequestByIdForHospital(id, email);
        MatchingEvaluationSummaryDTO summary = matchingService.getMatchesForRequest(id, email);

        model.addAttribute("bloodRequest", bloodRequest);
        model.addAttribute("summary", summary);
        model.addAttribute("userEmail", email);
        return "hospital/matches";
    }

    /**
     * Executes the donor matching engine for a blood request.
     */
    @RequestMapping(value = "/{id}/find-donors", method = {RequestMethod.GET, RequestMethod.POST})
    public String findDonors(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(id, email);
            redirectAttributes.addFlashAttribute("successMessage", summary.getMessage());
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Donor matching failed for request #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Matching execution failed: " + ex.getMessage());
        }
        return "redirect:/hospital/blood-requests/" + id + "/matches";
    }
}
