package com.donornexa.admin.controller;

import com.donornexa.audit.repository.AuditLogRepository;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.donation.repository.DonationRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.inventory.repository.BloodInventoryRepository;
import com.donornexa.matching.repository.RequestMatchRepository;
import com.donornexa.messaging.repository.ConversationRepository;
import com.donornexa.messaging.repository.MessageRepository;
import com.donornexa.notification.repository.NotificationRepository;
import com.donornexa.user.repository.UserRepository;

import com.donornexa.admin.dto.AdminPasswordChangeDTO;
import com.donornexa.admin.service.AdminDonorVerificationService;
import com.donornexa.admin.service.AdminProfileService;
import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.common.enums.DonationStatus;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.hospital.service.HospitalVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for the central administrator dashboard, overview metrics,
 * recent registrations, and activity stream.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final HospitalVerificationService hospitalVerificationService;
    private final AdminDonorVerificationService donorVerificationService;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdminProfileService adminProfileService;
    private final com.donornexa.messaging.repository.ConversationRepository conversationRepository;
    private final com.donornexa.messaging.repository.MessageRepository messageRepository;
    private final com.donornexa.donor.repository.DonorRepository donorRepository;
    private final com.donornexa.notification.repository.NotificationRepository notificationRepository;
    private final com.donornexa.matching.repository.RequestMatchRepository requestMatchRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("userEmail", userDetails.getUsername());

        // Platform-wide user metrics
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalDonors", donorVerificationService.getTotalDonorsCount());
        model.addAttribute("totalHospitals", hospitalVerificationService.getTotalHospitalsCount());

        // Verification metrics
        long pendingDonors = donorVerificationService.getPendingVerificationCount();
        long pendingHospitals = hospitalVerificationService.getPendingVerificationCount();
        long verifiedDonors = donorVerificationService.getVerifiedDonorsCount();
        long verifiedHospitals = hospitalVerificationService.getVerifiedHospitalsCount();
        long rejectedDonors = donorVerificationService.getRejectedDonorsCount();
        long rejectedHospitals = hospitalVerificationService.getRejectedHospitalsCount();

        model.addAttribute("pendingDonorVerification", pendingDonors);
        model.addAttribute("pendingHospitalVerification", pendingHospitals);
        model.addAttribute("verifiedDonors", verifiedDonors);
        model.addAttribute("verifiedHospitals", verifiedHospitals);
        model.addAttribute("rejectedApplications", rejectedDonors + rejectedHospitals);

        // Clinical & Operational metrics
        long requestsAwaitingApproval = bloodRequestRepository.countByStatusIn(
                List.of(RequestStatus.PENDING_ADMIN_APPROVAL, RequestStatus.PENDING));
        long approvedRequests = bloodRequestRepository.countByStatus(RequestStatus.APPROVED);
        long rejectedRequests = bloodRequestRepository.countByStatus(RequestStatus.REJECTED);
        long partiallyFulfilledRequests = bloodRequestRepository.countByStatus(RequestStatus.PARTIALLY_FULFILLED);
        long fulfilledRequests = bloodRequestRepository.countByStatus(RequestStatus.FULFILLED);
        long activeRequests = bloodRequestRepository.countByStatusIn(
                List.of(RequestStatus.PENDING_ADMIN_APPROVAL, RequestStatus.PENDING, RequestStatus.APPROVED,
                        RequestStatus.MATCHING, RequestStatus.MATCHING_IN_PROGRESS, RequestStatus.PARTIALLY_FULFILLED));

        model.addAttribute("totalBloodRequests", bloodRequestRepository.count());
        model.addAttribute("requestsAwaitingApproval", requestsAwaitingApproval);
        model.addAttribute("pendingBloodRequests", requestsAwaitingApproval);
        model.addAttribute("approvedBloodRequests", approvedRequests);
        model.addAttribute("rejectedBloodRequests", rejectedRequests);
        model.addAttribute("partiallyFulfilledBloodRequests", partiallyFulfilledRequests);
        model.addAttribute("fulfilledBloodRequests", fulfilledRequests);
        model.addAttribute("activeBloodRequests", activeRequests);
        model.addAttribute("completedBloodRequests", fulfilledRequests);

        model.addAttribute("totalDonorAssignments", requestMatchRepository.count());
        model.addAttribute("totalConfirmedUnits", bloodRequestRepository.sumTotalUnitsFulfilled());

        model.addAttribute("totalDonations", donationRepository.count());
        model.addAttribute("completedDonations", donationRepository.countByDonationStatus(DonationStatus.COMPLETED));
        model.addAttribute("totalInventoryUnits", bloodInventoryRepository.getTotalAvailableUnits());
        model.addAttribute("totalNotifications", notificationRepository.count());

        // Pending approval blood requests list
        List<BloodRequest> pendingApprovalList = bloodRequestRepository.findByStatusInWithDetails(
                List.of(RequestStatus.PENDING_ADMIN_APPROVAL, RequestStatus.PENDING));
        model.addAttribute("pendingApprovalRequests", pendingApprovalList);

        // Blood Group Distribution
        java.util.Map<String, Long> bloodGroupDistribution = new java.util.LinkedHashMap<>();
        for (com.donornexa.common.enums.BloodGroup bg : com.donornexa.common.enums.BloodGroup.values()) {
            bloodGroupDistribution.put(bg.name().replace('_', ' '), donorRepository.countByBloodGroup(bg));
        }
        model.addAttribute("bloodGroupDistribution", bloodGroupDistribution);

        // Coordination & Communication metrics
        model.addAttribute("totalConversations", conversationRepository.count());
        model.addAttribute("totalMessages", messageRepository.count());

        userRepository.findByEmailIgnoreCase(userDetails.getUsername()).ifPresent(adminUser -> {
            model.addAttribute("adminUnreadMessages", messageRepository.countByRecipientIdAndIsReadFalse(adminUser.getId()));
        });

        // Recent Activity, Blood Requests, and Registrations
        model.addAttribute("recentUsers", userRepository.findTop5ByOrderByCreatedAtDesc());
        model.addAttribute("recentBloodRequests", bloodRequestRepository.findTop5RecentWithDetails(org.springframework.data.domain.PageRequest.of(0, 5)));
        model.addAttribute("recentAuditLogs", auditLogRepository.findTop5WithActor(org.springframework.data.domain.PageRequest.of(0, 5)));

        return "admin/dashboard";
    }

    @GetMapping("/first-login-change-password")
    public String firstLoginChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("adminEmail", userDetails.getUsername());
        if (!model.containsAttribute("passwordDTO")) {
            model.addAttribute("passwordDTO", new AdminPasswordChangeDTO());
        }
        return "admin/first-login-change-password";
    }

    @PostMapping("/first-login-change-password")
    public String processFirstLoginPasswordChange(@Valid @ModelAttribute("passwordDTO") AdminPasswordChangeDTO passwordDTO,
                                                 BindingResult bindingResult,
                                                 @AuthenticationPrincipal UserDetails userDetails,
                                                 RedirectAttributes redirectAttributes,
                                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("adminEmail", userDetails.getUsername());
            model.addAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "admin/first-login-change-password";
        }

        try {
            adminProfileService.changeInitialTemporaryPassword(userDetails.getUsername(), passwordDTO);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your temporary password has been successfully updated. Welcome to the DonorNexa Administrator Dashboard!");
            return "redirect:/admin/dashboard";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("adminEmail", userDetails.getUsername());
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/first-login-change-password";
        }
    }
}
