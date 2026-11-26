package com.donornexa.messaging;

import com.donornexa.common.enums.Role;
import com.donornexa.user.entity.*;
import com.donornexa.donor.entity.*;
import com.donornexa.hospital.entity.*;
import com.donornexa.bloodrequest.entity.*;
import com.donornexa.matching.entity.*;
import com.donornexa.donation.entity.*;
import com.donornexa.inventory.entity.*;
import com.donornexa.notification.entity.*;
import com.donornexa.location.entity.*;
import com.donornexa.messaging.entity.*;
import com.donornexa.audit.entity.*;
import com.donornexa.admin.entity.*;
import com.donornexa.common.enums.*;
import com.donornexa.messaging.dto.ConversationSummaryDTO;
import com.donornexa.messaging.dto.MessageResponseDTO;
import com.donornexa.messaging.service.MessagingService;
import com.donornexa.user.repository.*;
import com.donornexa.donor.repository.*;
import com.donornexa.hospital.repository.*;
import com.donornexa.bloodrequest.repository.*;
import com.donornexa.matching.repository.*;
import com.donornexa.donation.repository.*;
import com.donornexa.inventory.repository.*;
import com.donornexa.notification.repository.*;
import com.donornexa.location.repository.*;
import com.donornexa.messaging.repository.*;
import com.donornexa.audit.repository.*;
import com.donornexa.admin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 9 â€” Comprehensive Automated Tests for Donorâ€“Hospital Communication & Messaging Module.
 * Verifies conversation creation eligibility, duplicate prevention, bidirectional text messaging,
 * read status receipts, in-app notification dispatch, IDOR multi-tenant isolation, Fetch API polling,
 * and role-based access restrictions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Phase 9 â€” Donorâ€“Hospital Communication & Messaging Tests")
class DonorHospitalMessagingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    @Autowired private MessagingService messagingService;

    private User donorUser1;
    private Donor donor1;
    private User donorUser2;
    private Donor donor2;

    private User hospitalUser1;
    private Hospital hospital1;
    private User hospitalUser2;
    private Hospital hospital2;

    private BloodRequest bloodRequest1;
    private RequestMatch match1;

    private Province province;
    private City city;

    @BeforeEach
    void setUp() {
        province = provinceRepository.findByName("Sindh").orElseGet(() -> {
            Province p = new Province();
            p.setName("Sindh");
            return provinceRepository.save(p);
        });

        city = cityRepository.findByName("Karachi").stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("Karachi");
            c.setProvince(province);
            return cityRepository.save(c);
        });

        // Hospital 1
        hospitalUser1 = new User();
        hospitalUser1.setEmail("chat.hosp1." + UUID.randomUUID() + "@donornexa.com");
        hospitalUser1.setPhoneNumber("0300" + (System.currentTimeMillis() % 10000000));
        hospitalUser1.setPasswordHash("hashed_pw");
        hospitalUser1.setRole(Role.HOSPITAL);
        hospitalUser1.setFullName("Aga Khan University Hospital");
        hospitalUser1.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser1 = userRepository.save(hospitalUser1);

        hospital1 = new Hospital();
        hospital1.setUser(hospitalUser1);
        hospital1.setHospitalName("Aga Khan University Hospital");
        hospital1.setRegistrationNumber("REG-MSG-" + UUID.randomUUID());
        hospital1.setProvince(province);
        hospital1.setCity(city);
        hospital1.setCompleteAddress("Stadium Road, Karachi");
        hospital1.setContactNumber("021-34930051");
        hospital1.setVerified(true);
        hospital1 = hospitalRepository.save(hospital1);

        // Hospital 2 (Unrelated hospital)
        hospitalUser2 = new User();
        hospitalUser2.setEmail("chat.hosp2." + UUID.randomUUID() + "@donornexa.com");
        hospitalUser2.setPhoneNumber("0301" + (System.currentTimeMillis() % 10000000));
        hospitalUser2.setPasswordHash("hashed_pw");
        hospitalUser2.setRole(Role.HOSPITAL);
        hospitalUser2.setFullName("Liaquat National Hospital");
        hospitalUser2.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser2 = userRepository.save(hospitalUser2);

        hospital2 = new Hospital();
        hospital2.setUser(hospitalUser2);
        hospital2.setHospitalName("Liaquat National Hospital");
        hospital2.setRegistrationNumber("REG-MSG2-" + UUID.randomUUID());
        hospital2.setProvince(province);
        hospital2.setCity(city);
        hospital2.setCompleteAddress("National Stadium Road, Karachi");
        hospital2.setContactNumber("021-34141414");
        hospital2.setVerified(true);
        hospital2 = hospitalRepository.save(hospital2);

        // Donor 1 (Matched to Hospital 1 request)
        donorUser1 = new User();
        donorUser1.setEmail("chat.donor1." + UUID.randomUUID() + "@donornexa.com");
        donorUser1.setPhoneNumber("0312" + (System.currentTimeMillis() % 10000000));
        donorUser1.setPasswordHash("hashed_pw");
        donorUser1.setRole(Role.DONOR);
        donorUser1.setFullName("Bilal Ahmed");
        donorUser1.setAccountStatus(AccountStatus.ACTIVE);
        donorUser1 = userRepository.save(donorUser1);

        donor1 = new Donor();
        donor1.setUser(donorUser1);
        donor1.setBloodGroup(BloodGroup.O_POSITIVE);
        donor1.setGender(Gender.MALE);
        donor1.setDateOfBirth(LocalDate.of(1996, 4, 10));
        donor1.setWeight(new BigDecimal("72.0"));
        donor1.setProvince(province);
        donor1.setCity(city);
        donor1.setCompleteAddress("Bahadurabad, Karachi");
        donor1.setVerified(true);
        donor1.setAvailability(DonorAvailability.AVAILABLE);
        donor1 = donorRepository.save(donor1);

        // Donor 2 (Unrelated donor)
        donorUser2 = new User();
        donorUser2.setEmail("chat.donor2." + UUID.randomUUID() + "@donornexa.com");
        donorUser2.setPhoneNumber("0313" + (System.currentTimeMillis() % 10000000));
        donorUser2.setPasswordHash("hashed_pw");
        donorUser2.setRole(Role.DONOR);
        donorUser2.setFullName("Hamza Siddiqui");
        donorUser2.setAccountStatus(AccountStatus.ACTIVE);
        donorUser2 = userRepository.save(donorUser2);

        donor2 = new Donor();
        donor2.setUser(donorUser2);
        donor2.setBloodGroup(BloodGroup.B_POSITIVE);
        donor2.setGender(Gender.MALE);
        donor2.setDateOfBirth(LocalDate.of(1998, 9, 12));
        donor2.setWeight(new BigDecimal("66.0"));
        donor2.setProvince(province);
        donor2.setCity(city);
        donor2.setCompleteAddress("Gulshan, Karachi");
        donor2.setVerified(true);
        donor2.setAvailability(DonorAvailability.AVAILABLE);
        donor2 = donorRepository.save(donor2);

        // Blood Request by Hospital 1
        bloodRequest1 = new BloodRequest();
        bloodRequest1.setHospital(hospital1);
        bloodRequest1.setBloodGroup(BloodGroup.O_POSITIVE);
        bloodRequest1.setUnitsRequired(2);
        bloodRequest1.setUnitsFulfilled(0);
        bloodRequest1.setUrgency(RequestUrgency.CRITICAL);
        bloodRequest1.setStatus(RequestStatus.MATCHING);
        bloodRequest1.setProvince(province);
        bloodRequest1.setCity(city);
        bloodRequest1.setRequiredBy(LocalDateTime.now().plusHours(12));
        bloodRequest1.setHospitalNotes("Emergency cardiac surgery coordination");
        bloodRequest1 = bloodRequestRepository.save(bloodRequest1);

        // Matched relationship between Donor 1 and Blood Request 1
        match1 = new RequestMatch();
        match1.setBloodRequest(bloodRequest1);
        match1.setDonor(donor1);
        match1.setMatchStatus(MatchStatus.ACCEPTED);
        match1.setMatchedAt(LocalDateTime.now());
        match1.setMatchScore(95);
        match1.setCompatibilitySummary("Exact ABO match â€¢ Same City");
        match1.setNotified(true);
        match1 = requestMatchRepository.save(match1);
    }

    @Test
    @DisplayName("Conversation creation succeeds for eligible matched donor and hospital")
    void testConversationCreation_ValidMatch_Success() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        assertThat(conversation).isNotNull();
        assertThat(conversation.getId()).isNotNull();
        assertThat(conversation.getDonor().getId()).isEqualTo(donor1.getId());
        assertThat(conversation.getHospital().getId()).isEqualTo(hospital1.getId());
        assertThat(conversation.getBloodRequest().getId()).isEqualTo(bloodRequest1.getId());
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Duplicate conversation creation is prevented (idempotent)")
    void testDuplicateConversationPrevention() {
        Conversation conv1 = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        Conversation conv2 = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), hospitalUser1.getEmail());

        assertThat(conv1.getId()).isEqualTo(conv2.getId());
        assertThat(conversationRepository.countByDonorId(donor1.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Conversation creation denied when no matching relationship exists")
    void testConversationCreation_UnrelatedUsers_ThrowsAccessDenied() {
        // Donor 2 has NOT been matched to Blood Request 1
        assertThatThrownBy(() -> messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor2.getId(), donorUser2.getEmail()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Eligibility Error");
    }

    @Test
    @DisplayName("Donor sends message to hospital: persists, updates timestamp, and triggers in-app notification")
    void testDonorSendsMessage_Success() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        long initialNotifs = notificationRepository.countByRecipientIdAndNotificationStatus(
                hospitalUser1.getId(), NotificationStatus.UNREAD);

        Message message = messagingService.sendMessage(
                conversation.getId(),
                "Hello, I can arrive at the blood bank today at 4:00 PM.",
                donorUser1.getEmail()
        );

        assertThat(message).isNotNull();
        assertThat(message.getId()).isNotNull();
        assertThat(message.getSender().getId()).isEqualTo(donorUser1.getId());
        assertThat(message.getRecipient().getId()).isEqualTo(hospitalUser1.getId());
        assertThat(message.getContent()).isEqualTo("Hello, I can arrive at the blood bank today at 4:00 PM.");
        assertThat(message.getIsRead()).isFalse();

        // Conversation lastMessageAt updated
        Conversation reloaded = conversationRepository.findById(conversation.getId()).orElseThrow();
        assertThat(reloaded.getLastMessageAt()).isNotNull();

        // Notification created for hospital recipient
        long postNotifs = notificationRepository.countByRecipientIdAndNotificationStatus(
                hospitalUser1.getId(), NotificationStatus.UNREAD);
        assertThat(postNotifs).isEqualTo(initialNotifs + 1);
    }

    @Test
    @DisplayName("Hospital sends message to donor: persists and triggers in-app notification")
    void testHospitalSendsMessage_Success() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), hospitalUser1.getEmail());

        Message message = messagingService.sendMessage(
                conversation.getId(),
                "Thank you Bilal. Please report to Room 204 in the Blood Bank building.",
                hospitalUser1.getEmail()
        );

        assertThat(message.getSender().getId()).isEqualTo(hospitalUser1.getId());
        assertThat(message.getRecipient().getId()).isEqualTo(donorUser1.getId());
        assertThat(message.getIsRead()).isFalse();

        // Notification created for donor
        List<Notification> notifs = notificationRepository.findByRecipientIdAndNotificationStatus(
                donorUser1.getId(), NotificationStatus.UNREAD);
        assertThat(notifs).isNotEmpty();
        assertThat(notifs.getFirst().getNotificationType()).isEqualTo(NotificationType.MESSAGE);
    }

    @Test
    @DisplayName("Retrieving conversation messages automatically marks incoming unread messages as read")
    void testRetrieveMessages_MarksUnreadAsRead() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        // Donor sends message to hospital
        messagingService.sendMessage(conversation.getId(), "Is fasting required?", donorUser1.getEmail());

        // Unread count for hospital is 1
        assertThat(messagingService.getUnreadMessageCountForUser(hospitalUser1.getEmail())).isEqualTo(1);

        // Hospital opens conversation and retrieves messages
        List<MessageResponseDTO> messages = messagingService.getMessagesAndMarkAsRead(
                conversation.getId(), hospitalUser1.getEmail());

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().isSender()).isFalse();

        // Unread count for hospital is now 0
        assertThat(messagingService.getUnreadMessageCountForUser(hospitalUser1.getEmail())).isEqualTo(0);

        // In DB, isRead is true
        Message dbMsg = messageRepository.findById(messages.getFirst().getId()).orElseThrow();
        assertThat(dbMsg.getIsRead()).isTrue();
        assertThat(dbMsg.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Reject blank or oversized message submissions")
    void testMessageValidation_BlankOrTooLong() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        assertThatThrownBy(() -> messagingService.sendMessage(conversation.getId(), "   ", donorUser1.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");

        String hugeText = "A".repeat(2001);
        assertThatThrownBy(() -> messagingService.sendMessage(conversation.getId(), hugeText, donorUser1.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    @DisplayName("IDOR protection: Donor cannot access another donor's conversation")
    void testDonorAccessOnlyOwnConversation_IDORProtection() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        // Donor 2 attempts to inspect Donor 1's conversation
        assertThatThrownBy(() -> messagingService.getConversationByIdAndVerifyAccess(
                conversation.getId(), donorUser2.getEmail()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied");
    }

    @Test
    @DisplayName("IDOR protection: Hospital cannot access another hospital's conversation")
    void testHospitalAccessOnlyOwnConversation_IDORProtection() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), hospitalUser1.getEmail());

        // Hospital 2 attempts to inspect Hospital 1's conversation
        assertThatThrownBy(() -> messagingService.getConversationByIdAndVerifyAccess(
                conversation.getId(), hospitalUser2.getEmail()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access Denied");
    }

    @Test
    @DisplayName("Cannot send message in closed conversation")
    void testClosedConversation_CannotReceiveMessages() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        conversation.setStatus(ConversationStatus.CLOSED);
        conversationRepository.save(conversation);

        assertThatThrownBy(() -> messagingService.sendMessage(
                conversation.getId(), "Hello after close", donorUser1.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Donor and Hospital inboxes return summaries with latest messages and unread counts")
    void testInboxSummaryRetrieval() {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        messagingService.sendMessage(conversation.getId(), "Are you available tomorrow?", hospitalUser1.getEmail());

        List<ConversationSummaryDTO> donorInbox = messagingService.getDonorConversations(donorUser1.getEmail());
        assertThat(donorInbox).hasSize(1);
        assertThat(donorInbox.getFirst().getUnreadCount()).isEqualTo(1);
        assertThat(donorInbox.getFirst().getLastMessagePreview()).contains("Are you available tomorrow?");

        List<ConversationSummaryDTO> hospitalInbox = messagingService.getHospitalConversations(hospitalUser1.getEmail());
        assertThat(hospitalInbox).hasSize(1);
        assertThat(hospitalInbox.getFirst().getUnreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Web endpoint: Donor inbox returns 200 OK with model attributes")
    void testDonorInboxWebEndpoint() throws Exception {
        mockMvc.perform(get("/donor/messages")
                        .with(user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/messages/inbox"))
                .andExpect(model().attributeExists("conversations", "unreadMessageCount"));
    }

    @Test
    @DisplayName("Web endpoint: Hospital inbox returns 200 OK with model attributes")
    void testHospitalInboxWebEndpoint() throws Exception {
        mockMvc.perform(get("/hospital/messages")
                        .with(user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/messages/inbox"))
                .andExpect(model().attributeExists("conversations", "unreadMessageCount"));
    }

    @Test
    @DisplayName("Web endpoint: Chat conversation view returns 200 OK")
    void testChatConversationWebEndpoint() throws Exception {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        mockMvc.perform(get("/donor/messages/" + conversation.getId())
                        .with(user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/messages/chat"))
                .andExpect(model().attributeExists("conversation", "messages", "sendMessageDTO"));
    }

    @Test
    @DisplayName("Web endpoint: Fetch API history polling endpoint returns JSON message list")
    void testPollHistoryEndpoint_ReturnsJson() throws Exception {
        Conversation conversation = messagingService.getOrCreateConversation(
                bloodRequest1.getId(), donor1.getId(), donorUser1.getEmail());

        messagingService.sendMessage(conversation.getId(), "Realtime polling message", donorUser1.getEmail());

        mockMvc.perform(get("/donor/messages/" + conversation.getId() + "/history")
                        .with(user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].content").value("Realtime polling message"));
    }

    @Test
    @DisplayName("Role security: Donor cannot access hospital messages (403 Forbidden)")
    void testRoleRestriction_DonorCannotAccessHospitalMessages() throws Exception {
        mockMvc.perform(get("/hospital/messages")
                        .with(user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Role security: Hospital cannot access donor messages (403 Forbidden)")
    void testRoleRestriction_HospitalCannotAccessDonorMessages() throws Exception {
        mockMvc.perform(get("/donor/messages")
                        .with(user(hospitalUser1.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isForbidden());
    }
}
