package com.donornexa.messaging.service;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.bloodrequest.repository.BloodRequestRepository;
import com.donornexa.donor.entity.Donor;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.matching.entity.RequestMatch;
import com.donornexa.matching.repository.RequestMatchRepository;
import com.donornexa.messaging.entity.Conversation;
import com.donornexa.messaging.entity.Message;
import com.donornexa.messaging.repository.ConversationRepository;
import com.donornexa.messaging.repository.MessageRepository;
import com.donornexa.user.entity.User;
import com.donornexa.user.repository.UserRepository;

import com.donornexa.common.enums.Role;
import com.donornexa.common.enums.ConversationStatus;
import com.donornexa.common.enums.NotificationType;
import com.donornexa.messaging.dto.ConversationSummaryDTO;
import com.donornexa.messaging.dto.MessageResponseDTO;
import com.donornexa.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RequestMatchRepository requestMatchRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorRepository donorRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Retrieves an existing conversation or creates a new one after validating
     * communication eligibility via an active RequestMatch.
     */
    @Transactional
    public Conversation getOrCreateConversation(Long bloodRequestId, Long donorId, String userEmail) {
        User currentUser = getUserByEmail(userEmail);

        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new NoSuchElementException("Blood request not found with ID: " + bloodRequestId));

        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new NoSuchElementException("Donor not found with ID: " + donorId));

        Hospital hospital = bloodRequest.getHospital();

        // Security check: currentUser must be either the donor, the hospital user, or an administrator
        boolean isDonor = currentUser.getId().equals(donor.getUser().getId());
        boolean isHospital = currentUser.getId().equals(hospital.getUser().getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isDonor && !isHospital && !isAdmin) {
            log.warn("Unauthorized attempt to initiate conversation between Donor {} and Hospital {} by user {}",
                    donorId, hospital.getId(), userEmail);
            throw new AccessDeniedException("Access Denied: You are not an authorized participant for this coordination channel.");
        }

        // Eligibility check: A RequestMatch must exist between donor and bloodRequest (unless Admin)
        if (!isAdmin) {
            boolean matchExists = requestMatchRepository.existsByDonorIdAndBloodRequestId(donorId, bloodRequestId);
            if (!matchExists) {
                log.warn("Communication denied: No matching relation between donor {} and blood request {}", donorId, bloodRequestId);
                throw new AccessDeniedException("Communication Eligibility Error: Messaging is only permitted between matched donors and hospitals for an active coordination request.");
            }
        }

        // Prevent duplicate conversations
        Optional<Conversation> existing = conversationRepository
                .findByDonorIdAndHospitalIdAndBloodRequestId(donorId, hospital.getId(), bloodRequestId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new conversation
        Conversation conversation = new Conversation();
        conversation.setDonor(donor);
        conversation.setHospital(hospital);
        conversation.setBloodRequest(bloodRequest);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setLastMessageAt(LocalDateTime.now());

        Conversation saved = conversationRepository.save(conversation);
        log.info("Created new conversation ID {} between Donor {} and Hospital {} for Request #{}",
                saved.getId(), donor.getId(), hospital.getId(), bloodRequestId);
        return saved;
    }

    /**
     * Find conversation by ID with participant ownership verification (IDOR protection).
     */
    @Transactional(readOnly = true)
    public Conversation getConversationByIdAndVerifyAccess(Long conversationId, String userEmail) {
        User currentUser = getUserByEmail(userEmail);

        Conversation conversation = conversationRepository.findByIdWithDetails(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found with ID: " + conversationId));

        boolean isDonor = currentUser.getId().equals(conversation.getDonor().getUser().getId());
        boolean isHospital = currentUser.getId().equals(conversation.getHospital().getUser().getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isDonor && !isHospital && !isAdmin) {
            log.warn("IDOR attempt: User {} attempted to access conversation ID {} without authorization",
                    userEmail, conversationId);
            throw new AccessDeniedException("Access Denied: You are not authorized to view this conversation.");
        }

        return conversation;
    }

    /**
     * Send a text message inside an authorized conversation.
     */
    @Transactional
    public Message sendMessage(Long conversationId, String rawContent, String userEmail) {
        return sendMessage(conversationId, rawContent, userEmail, null);
    }

    /**
     * Send a text message inside an authorized conversation with optional target recipient override
     * (used by administrators mediating coordination).
     */
    @Transactional
    public Message sendMessage(Long conversationId, String rawContent, String userEmail, Long targetRecipientId) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank.");
        }

        String content = rawContent.trim();
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Message exceeds maximum length of 2000 characters.");
        }

        User sender = getUserByEmail(userEmail);
        Conversation conversation = getConversationByIdAndVerifyAccess(conversationId, userEmail);

        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new IllegalStateException("This conversation has been closed and cannot receive new messages.");
        }

        User recipient;
        if (sender.getId().equals(conversation.getDonor().getUser().getId())) {
            recipient = conversation.getHospital().getUser();
        } else if (sender.getId().equals(conversation.getHospital().getUser().getId())) {
            recipient = conversation.getDonor().getUser();
        } else if (sender.getRole() == Role.ADMIN) {
            // Admin sending message
            if (targetRecipientId != null) {
                recipient = userRepository.findById(targetRecipientId)
                        .orElse(conversation.getHospital().getUser());
            } else {
                recipient = conversation.getHospital().getUser();
            }
        } else {
            throw new AccessDeniedException("Access Denied: Only conversation participants or administrators may send messages.");
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setIsRead(false);
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        // Update last message timestamp
        conversation.setLastMessageAt(savedMessage.getSentAt());
        conversationRepository.save(conversation);

        // Create in-app notification for the recipient
        String preview = content.length() > 100 ? content.substring(0, 97) + "..." : content;
        String senderLabel = (sender.getRole() == Role.ADMIN) ? "Administrator (" + sender.getFullName() + ")" : sender.getFullName();
        notificationService.createNotification(
                recipient,
                "New Message from " + senderLabel,
                "Regarding Blood Request #" + conversation.getBloodRequest().getId() + ": " + preview,
                NotificationType.MESSAGE
        );

        log.info("Message ID {} sent in conversation ID {} from {} to {}",
                savedMessage.getId(), conversationId, sender.getEmail(), recipient.getEmail());

        return savedMessage;
    }

    /**
     * Send message and return standardized DTO (ideal for AJAX / Fetch API interactions).
     */
    @Transactional
    public MessageResponseDTO sendMessageAndReturnDTO(Long conversationId, String rawContent, String userEmail, Long targetRecipientId) {
        Message saved = sendMessage(conversationId, rawContent, userEmail, targetRecipientId);
        return MessageResponseDTO.builder()
                .id(saved.getId())
                .conversationId(conversationId)
                .senderId(saved.getSender().getId())
                .senderName(saved.getSender().getFullName())
                .senderRole(saved.getSender().getRole().name())
                .content(saved.getContent())
                .sentAt(saved.getSentAt())
                .isSender(true)
                .isRead(false)
                .build();
    }

    /**
     * Retrieves messages for a conversation and marks unread messages as read.
     */
    @Transactional
    public List<MessageResponseDTO> getMessagesAndMarkAsRead(Long conversationId, String userEmail) {
        User currentUser = getUserByEmail(userEmail);
        Conversation conversation = getConversationByIdAndVerifyAccess(conversationId, userEmail);

        // Mark incoming unread messages as read
        List<Message> unreadList = messageRepository.findByConversationIdAndRecipientIdAndIsReadFalse(
                conversationId, currentUser.getId());
        if (!unreadList.isEmpty()) {
            for (Message m : unreadList) {
                m.markAsRead();
            }
            messageRepository.saveAll(unreadList);
        }

        // Fetch all messages sorted ascending
        List<Message> messages = messageRepository.findByConversationIdWithSender(conversationId);
        List<MessageResponseDTO> dtos = new ArrayList<>();

        for (Message m : messages) {
            boolean isSender = m.getSender().getId().equals(currentUser.getId());
            dtos.add(MessageResponseDTO.builder()
                    .id(m.getId())
                    .conversationId(conversationId)
                    .senderId(m.getSender().getId())
                    .senderName(m.getSender().getFullName())
                    .senderRole(m.getSender().getRole().name())
                    .content(m.getContent())
                    .sentAt(m.getSentAt())
                    .isSender(isSender)
                    .isRead(Boolean.TRUE.equals(m.getIsRead()))
                    .build());
        }

        return dtos;
    }

    /**
     * Retrieves inbox conversations for a registered donor.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getDonorConversations(String donorEmail) {
        Donor donor = donorRepository.findByUserEmail(donorEmail.toLowerCase().trim())
                .orElseThrow(() -> new NoSuchElementException("Donor profile not found for email: " + donorEmail));

        List<Conversation> conversations = conversationRepository.findByDonorIdWithDetails(donor.getId());
        return mapToSummaryDTOList(conversations, donor.getUser().getId());
    }

    /**
     * Retrieves inbox conversations for a registered hospital.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getHospitalConversations(String hospitalEmail) {
        Hospital hospital = hospitalRepository.findByUserEmailWithDetails(hospitalEmail.toLowerCase().trim())
                .orElseThrow(() -> new NoSuchElementException("Hospital profile not found for email: " + hospitalEmail));

        List<Conversation> conversations = conversationRepository.findByHospitalIdWithDetails(hospital.getId());
        return mapToSummaryDTOList(conversations, hospital.getUser().getId());
    }

    /**
     * Retrieves all active coordination conversations across the platform for Administrator oversight.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getAdminConversations(String adminEmail) {
        User admin = getUserByEmail(adminEmail);
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access Denied: Only administrators can access system-wide conversations.");
        }
        List<Conversation> conversations = conversationRepository.findAllWithDetails();
        return mapToSummaryDTOList(conversations, admin.getId());
    }

    /**
     * Count unread messages for the current user across all conversations.
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCountForUser(String email) {
        User user = getUserByEmail(email);
        return messageRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional(readOnly = true)
    public long getTotalConversationsCount() {
        return conversationRepository.count();
    }

    @Transactional(readOnly = true)
    public long getTotalMessagesCount() {
        return messageRepository.count();
    }

    private List<ConversationSummaryDTO> mapToSummaryDTOList(List<Conversation> conversations, Long currentUserId) {
        List<ConversationSummaryDTO> list = new ArrayList<>();
        for (Conversation c : conversations) {
            long unread = messageRepository.countByConversationIdAndRecipientIdAndIsReadFalse(c.getId(), currentUserId);
            Optional<Message> lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(c.getId());

            String preview = lastMessage.map(Message::getContent).orElse("No messages yet.");
            if (preview.length() > 60) {
                preview = preview.substring(0, 57) + "...";
            }

            list.add(ConversationSummaryDTO.builder()
                    .id(c.getId())
                    .donorId(c.getDonor().getId())
                    .donorName(c.getDonor().getUser().getFullName())
                    .hospitalId(c.getHospital().getId())
                    .hospitalName(c.getHospital().getHospitalName())
                    .bloodRequestId(c.getBloodRequest().getId())
                    .bloodGroup(c.getBloodRequest().getBloodGroup())
                    .lastMessagePreview(preview)
                    .lastMessageAt(c.getLastMessageAt())
                    .unreadCount(unread)
                    .status(c.getStatus())
                    .build());
        }
        return list;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NoSuchElementException("User account not found: " + email));
    }
}
