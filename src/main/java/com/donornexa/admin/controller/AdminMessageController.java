package com.donornexa.admin.controller;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.messaging.entity.Conversation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.messaging.dto.ConversationSummaryDTO;
import com.donornexa.messaging.dto.MessageResponseDTO;
import com.donornexa.messaging.dto.SendMessageDTO;
import com.donornexa.messaging.service.MessagingService;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.donor.repository.DonorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controller providing authorized administrative oversight and mediation of coordination conversations
 * between registered voluntary donors, hospitals, and administrative staff.
 */
@Slf4j
@Controller
@RequestMapping("/admin/messages")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessagingService messagingService;
    private final NotificationService notificationService;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorRepository donorRepository;

    @GetMapping
    public String viewInbox(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        List<ConversationSummaryDTO> conversations = messagingService.getAdminConversations(email);
        long unreadCount = messagingService.getUnreadMessageCountForUser(email);
        long unreadNotifications = notificationService.getUnreadCount(email);

        model.addAttribute("conversations", conversations);
        model.addAttribute("unreadMessageCount", unreadCount);
        model.addAttribute("unreadNotificationCount", unreadNotifications);
        model.addAttribute("userEmail", email);
        model.addAttribute("activeTab", "messages");
        return "admin/messages/inbox";
    }

    @GetMapping("/{id}")
    public String viewConversation(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable("id") Long conversationId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            Conversation conversation = messagingService.getConversationByIdAndVerifyAccess(conversationId, email);
            List<MessageResponseDTO> messages = messagingService.getMessagesAndMarkAsRead(conversationId, email);
            long unreadNotifications = notificationService.getUnreadCount(email);
            long unreadMessages = messagingService.getUnreadMessageCountForUser(email);

            model.addAttribute("conversation", conversation);
            model.addAttribute("messages", messages);
            model.addAttribute("sendMessageDTO", new SendMessageDTO());
            model.addAttribute("unreadMessageCount", unreadMessages);
            model.addAttribute("unreadNotificationCount", unreadNotifications);
            model.addAttribute("userEmail", email);
            model.addAttribute("activeTab", "messages");
            return "admin/messages/chat";
        } catch (Exception ex) {
            log.warn("Admin error accessing conversation ID {}: {}", conversationId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/messages";
        }
    }

    @PostMapping("/{id}")
    public String sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable("id") Long conversationId,
                              @RequestParam(value = "targetRecipientId", required = false) Long targetRecipientId,
                              @Valid @ModelAttribute("sendMessageDTO") SendMessageDTO form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Invalid message content.";
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/messages/" + conversationId;
        }

        try {
            messagingService.sendMessage(conversationId, form.getContent(), email, targetRecipientId);
        } catch (Exception ex) {
            log.warn("Admin failed to send message in conversation {}: {}", conversationId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/messages/" + conversationId;
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<List<MessageResponseDTO>> getMessageHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long conversationId) {
        String email = userDetails.getUsername();
        List<MessageResponseDTO> messages = messagingService.getMessagesAndMarkAsRead(conversationId, email);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{id}/send-ajax")
    @ResponseBody
    public ResponseEntity<?> sendMessageAjax(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long conversationId,
            @RequestParam(value = "targetRecipientId", required = false) Long targetRecipientId,
            @RequestBody SendMessageDTO form) {
        String email = userDetails.getUsername();
        if (form.getContent() == null || form.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content cannot be blank.");
        }
        try {
            MessageResponseDTO dto = messagingService.sendMessageAndReturnDTO(
                    conversationId, form.getContent(), email, targetRecipientId);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/initiate")
    public String initiateConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("bloodRequestId") Long bloodRequestId,
            @RequestParam("donorId") Long donorId,
            RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        try {
            BloodRequest request = bloodRequestRepository.findById(bloodRequestId)
                    .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + bloodRequestId));
            Donor donor = donorRepository.findById(donorId)
                    .orElseThrow(() -> new NoSuchElementException("Donor not found with ID: " + donorId));

            Conversation conversation = messagingService.getOrCreateConversation(
                    request.getId(),
                    donor.getId(),
                    email
            );

            return "redirect:/admin/messages/" + conversation.getId();
        } catch (Exception ex) {
            log.warn("Failed to initiate admin conversation for request {}: {}", bloodRequestId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/messages";
        }
    }
}
