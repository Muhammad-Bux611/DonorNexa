package com.donornexa.donor.controller;

import com.donornexa.matching.dto.DonorMatchDTO;
import com.donornexa.matching.service.DonorMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller managing blood donation matches and response actions from the donor portal.
 */
@Slf4j
@Controller
@RequestMapping("/donor/matches")
@RequiredArgsConstructor
public class DonorMatchController {

    private final DonorMatchingService matchingService;

    @GetMapping
    public String listMyMatches(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {
        String email = userDetails.getUsername();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));

        Page<DonorMatchDTO> matchPage = matchingService.getMatchesForDonor(email, pageable);

        model.addAttribute("matchPage", matchPage);
        model.addAttribute("userEmail", email);
        return "donor/matches";
    }

    @PostMapping("/{id}/accept")
    public String acceptMatch(@PathVariable("id") Long id,
                              @RequestParam(value = "notes", required = false) String notes,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            matchingService.respondToMatch(id, email, true, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you! You have accepted the blood donation match. The hospital has been notified and may contact you.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to accept match #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept match: " + ex.getMessage());
        }
        return "redirect:/donor/matches";
    }

    @PostMapping("/{id}/decline")
    public String declineMatch(@PathVariable("id") Long id,
                               @RequestParam(value = "notes", required = false) String notes,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            matchingService.respondToMatch(id, email, false, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "You have declined this donation match. The hospital has been notified.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to decline match #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to decline match: " + ex.getMessage());
        }
        return "redirect:/donor/matches";
    }
}
