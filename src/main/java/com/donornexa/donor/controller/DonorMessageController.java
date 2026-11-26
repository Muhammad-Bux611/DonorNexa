package com.donornexa.donor.controller;

import com.donornexa.messaging.entity.Conversation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.messaging.dto.ConversationSummaryDTO;
import com.donornexa.messaging.dto.MessageResponseDTO;
import com.donornexa.messaging.dto.SendMessageDTO;
import com.donornexa.messaging.service.MessagingService;
import com.donornexa.notification.service.NotificationService;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.matching.repository.RequestMatchRepository;
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

@Slf4j
@Controller
@RequestMapping("/donor/messages")
@PreAuthorize("hasRole('DONOR')")
@RequiredArgsConstructor
public class DonorMessageController {

    private final MessagingService messagingService;
    private final NotificationService notificationService;
    private final RequestMatchRepository requestMatchRepository;
    private final DonorRepository donorRepository;

    @GetMapping
    public String viewInbox(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String email = userDetails.getUsername();
        List<ConversationSummaryDTO> conversations = messagingService.getDonorConversations(email);
        long unreadCount = messagingService.getUnreadMessageCountForUser(email);
        long unreadNotifications = notificationService.getUnreadCount(email);

        Donor donor = donorRepository.findByUserEmail(email).orElse(null);

        model.addAttribute("conversations", conversations);
        model.addAttribute("unreadMessageCount", unreadCount);
        model.addAttribute("unreadNotificationCount", unreadNotifications);
        model.addAttribute("donor", donor);
        model.addAttribute("userEmail", email);
        model.addAttribute("activeTab", "messages");

        // If conversations exist, auto-select the first conversation for immediate viewing
        if (conversations != null && !conversations.isEmpty()) {
            Long firstId = conversations.get(0).getId();
            try {
                Conversation conversation = messagingService.getConversationByIdAndVerifyAccess(firstId, email);
                List<MessageResponseDTO> messages = messagingService.getMessagesAndMarkAsRead(firstId, email);
                model.addAttribute("conversation", conversation);
                model.addAttribute("messages", messages);
                model.addAttribute("selectedConversationId", firstId);
            } catch (Exception ex) {
                log.warn("Could not auto-select first conversation for donor {}: {}", email, ex.getMessage());
            }
        }
        model.addAttribute("sendMessageDTO", new SendMessageDTO());

        return "donor/messages/inbox";
    }

    @GetMapping("/{id}")
    public String viewConversation(@AuthenticationPrincipal UserDetails userDetails,
                                   @PathVariable("id") Long conversationId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();
        try {
            List<ConversationSummaryDTO> conversations = messagingService.getDonorConversations(email);
            Conversation conversation = messagingService.getConversationByIdAndVerifyAccess(conversationId, email);
            List<MessageResponseDTO> messages = messagingService.getMessagesAndMarkAsRead(conversationId, email);
            long unreadNotifications = notificationService.getUnreadCount(email);
            long unreadMessages = messagingService.getUnreadMessageCountForUser(email);

            Donor donor = donorRepository.findByUserEmail(email).orElse(null);

            model.addAttribute("conversations", conversations);
            model.addAttribute("conversation", conversation);
            model.addAttribute("messages", messages);
            model.addAttribute("selectedConversationId", conversationId);
            model.addAttribute("sendMessageDTO", new SendMessageDTO());
            model.addAttribute("unreadMessageCount", unreadMessages);
            model.addAttribute("unreadNotificationCount", unreadNotifications);
            model.addAttribute("donor", donor);
            model.addAttribute("userEmail", email);
            model.addAttribute("activeTab", "messages");
            return "donor/messages/chat";
        } catch (Exception ex) {
            log.warn("Error accessing conversation ID {}: {}", conversationId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/donor/messages";
        }
    }

    @PostMapping("/{id}")
    public String sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable("id") Long conversationId,
                              @Valid @ModelAttribute("sendMessageDTO") SendMessageDTO form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Invalid message content.";
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/donor/messages/" + conversationId;
        }

        try {
            messagingService.sendMessage(conversationId, form.getContent(), email);
        } catch (Exception ex) {
            log.warn("Failed to send message in conversation {}: {}", conversationId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/donor/messages/" + conversationId;
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
            @RequestBody SendMessageDTO form) {
        String email = userDetails.getUsername();
        if (form.getContent() == null || form.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content cannot be blank.");
        }
        try {
            MessageResponseDTO dto = messagingService.sendMessageAndReturnDTO(
                    conversationId, form.getContent(), email, null);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/initiate")
    public String initiateConversationFromMatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("matchId") Long matchId,
            RedirectAttributes redirectAttributes) {
        String email = userDetails.getUsername();

        try {
            RequestMatch match = requestMatchRepository.findByIdWithDetails(matchId)
                    .orElseThrow(() -> new NoSuchElementException("Blood request match not found with ID: " + matchId));

            Conversation conversation = messagingService.getOrCreateConversation(
                    match.getBloodRequest().getId(),
                    match.getDonor().getId(),
                    email
            );

            return "redirect:/donor/messages/" + conversation.getId();
        } catch (Exception ex) {
            log.warn("Failed to initiate conversation from match {}: {}", matchId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/donor/matches";
        }
    }
}
