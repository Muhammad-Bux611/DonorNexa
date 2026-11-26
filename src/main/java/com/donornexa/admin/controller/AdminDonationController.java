package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminDonationService;
import com.donornexa.donation.entity.Donation;
import com.donornexa.common.enums.DonationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

/**
 * Controller for administrative viewing and inspection of donation records.
 */
@Slf4j
@Controller
@RequestMapping("/admin/donations")
@RequiredArgsConstructor
public class AdminDonationController {

    private final AdminDonationService adminDonationService;

    @GetMapping
    public String listDonations(@RequestParam(value = "query", required = false) String query,
                                @RequestParam(value = "status", required = false) DonationStatus status,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("donationDate").descending());

        Page<Donation> donationPage = adminDonationService.searchDonations(query, status, pageable);

        model.addAttribute("donationPage", donationPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", DonationStatus.values());
        model.addAttribute("completedCount", adminDonationService.getCompletedDonationsCount());

        return "admin/donations";
    }

    @GetMapping("/{id}")
    public String viewDonationDetails(@PathVariable("id") Long id,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            Donation donation = adminDonationService.getDonationById(id);
            model.addAttribute("donation", donation);
            return "admin/donation-details";
        } catch (NoSuchElementException ex) {
            log.error("Donation lookup failed for ID {}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/donations";
        }
    }
}
