package com.donornexa.admin.controller;

import com.donornexa.admin.service.AdminCommunicationService;
import com.donornexa.notification.entity.Notification;
import com.donornexa.common.enums.NotificationStatus;
import com.donornexa.common.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for centralized administrative oversight of notifications delivery
 * and coordination messaging metrics across the platform.
 */
@Slf4j
@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCommunicationController {

    private final AdminCommunicationService adminCommunicationService;

    @GetMapping
    public String viewCommunicationsOversight(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "status", required = false) NotificationStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        int validPage = Math.max(0, page);
        int validSize = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(validPage, validSize);

        Page<Notification> notificationPage = adminCommunicationService.getNotificationsPage(type, status, pageable);

        model.addAttribute("notificationPage", notificationPage);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("statuses", NotificationStatus.values());

        // Communication KPI metrics
        model.addAttribute("totalNotifications", adminCommunicationService.getTotalNotificationsCount());
        model.addAttribute("unreadNotifications", adminCommunicationService.getUnreadNotificationsCount());
        model.addAttribute("totalConversations", adminCommunicationService.getTotalConversationsCount());
        model.addAttribute("activeConversations", adminCommunicationService.getActiveConversationsCount());
        model.addAttribute("totalMessages", adminCommunicationService.getTotalMessagesCount());
        model.addAttribute("unreadMessages", adminCommunicationService.getUnreadMessagesCount());

        model.addAttribute("activeTab", "notifications");
        model.addAttribute("userEmail", userDetails != null ? userDetails.getUsername() : "");

        return "admin/notifications";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/read")
    public String markNotificationRead(@org.springframework.web.bind.annotation.PathVariable("id") Long id,
                                       @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                       org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            adminCommunicationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("successMessage", "Notification #" + id + " marked as read.");
        } catch (Exception ex) {
            log.error("Failed to mark notification {} read: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark notification as read.");
        }
        return redirectUrl != null && !redirectUrl.isBlank() ? "redirect:" + redirectUrl : "redirect:/admin/notifications";
    }

    @org.springframework.web.bind.annotation.PostMapping("/read-all")
    public String markAllNotificationsRead(org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            int marked = adminCommunicationService.markAllAsRead();
            redirectAttributes.addFlashAttribute("successMessage",
                    marked > 0 ? "Marked " + marked + " notification(s) as read." : "All notifications are already read.");
        } catch (Exception ex) {
            log.error("Failed to mark all notifications read: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to mark all notifications as read.");
        }
        return "redirect:/admin/notifications";
    }
}
