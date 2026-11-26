package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminDonorVerificationService;
import com.donornexa.hospital.service.HospitalVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller providing verification navigation and triage hubs across donors and hospitals.
 * Supports /admin/verification and /admin/verifications paths.
 */
@Controller
@RequestMapping({"/admin/verification", "/admin/verifications"})
@RequiredArgsConstructor
public class AdminVerificationController {

    private final AdminDonorVerificationService donorVerificationService;
    private final HospitalVerificationService hospitalVerificationService;

    @GetMapping
    public String verificationHub(Model model) {
        model.addAttribute("pendingDonors", donorVerificationService.getPendingVerificationCount());
        model.addAttribute("pendingHospitals", hospitalVerificationService.getPendingVerificationCount());

        model.addAttribute("verifiedDonors", donorVerificationService.getVerifiedDonorsCount());
        model.addAttribute("verifiedHospitals", hospitalVerificationService.getVerifiedHospitalsCount());

        model.addAttribute("rejectedDonors", donorVerificationService.getRejectedDonorsCount());
        model.addAttribute("rejectedHospitals", hospitalVerificationService.getRejectedHospitalsCount());

        return "admin/verification";
    }

    @GetMapping({"/donors", "/donor"})
    public String donorVerifications() {
        return "redirect:/admin/donors?verificationStatus=PENDING";
    }

    @GetMapping({"/hospitals", "/hospital"})
    public String hospitalVerifications() {
        return "redirect:/admin/hospitals?verificationStatus=PENDING";
    }

    @GetMapping("/verified")
    public String verifiedOverview() {
        return "redirect:/admin/hospitals?verificationStatus=VERIFIED";
    }

    @GetMapping("/rejected")
    public String rejectedOverview() {
        return "redirect:/admin/hospitals?verificationStatus=REJECTED";
    }
}
