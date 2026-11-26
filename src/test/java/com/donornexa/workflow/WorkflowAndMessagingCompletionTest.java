package com.donornexa.workflow;

import com.donornexa.admin.service.AdminBloodRequestService;
import com.donornexa.common.enums.Role;
import com.donornexa.donor.dto.DonorDashboardSummary;
import com.donornexa.donor.service.DonorService;
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
import com.donornexa.hospital.dto.HospitalDashboardSummary;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalBloodRequestService;
import com.donornexa.hospital.service.HospitalDonationService;
import com.donornexa.hospital.service.HospitalService;
import com.donornexa.matching.service.DonorMatchingService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("DonorNexa Complete Workflow & Messaging Lifecycle Integration Tests")
class WorkflowAndMessagingCompletionTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    @Autowired private AdminBloodRequestService adminBloodRequestService;
    @Autowired private DonorMatchingService donorMatchingService;
    @Autowired private HospitalDonationService hospitalDonationService;
    @Autowired private MessagingService messagingService;
    @Autowired private HospitalService hospitalService;
    @Autowired private DonorService donorService;

    private Province province;
    private City city;

    private User adminUser;
    private User hospitalUser;
    private Hospital hospital;

    private User donorUserA;
    private Donor donorA;

    private User donorUserB;
    private Donor donorB;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        province = provinceRepository.findAll().stream().findFirst().orElseGet(() -> {
            Province p = new Province();
            p.setName("Province-" + uid);
            return provinceRepository.save(p);
        });

        city = cityRepository.findAll().stream().findFirst().orElseGet(() -> {
            City c = new City();
            c.setName("City-" + uid);
            c.setProvince(province);
            return cityRepository.save(c);
        });

        // Admin User
        adminUser = new User();
        adminUser.setEmail("admin-" + uid + "@donornexa.com");
        adminUser.setPhoneNumber("0300-1" + uid.substring(0, 6));
        adminUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        adminUser.setFullName("Platform Admin");
        adminUser.setRole(Role.ADMIN);
        adminUser.setAccountStatus(AccountStatus.ACTIVE);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);

        // Hospital User & Profile
        hospitalUser = new User();
        hospitalUser.setEmail("hospital-" + uid + "@donornexa.com");
        hospitalUser.setPhoneNumber("0300-2" + uid.substring(0, 6));
        hospitalUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        hospitalUser.setFullName("City Emergency Hospital");
        hospitalUser.setRole(Role.HOSPITAL);
        hospitalUser.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser.setEnabled(true);
        hospitalUser = userRepository.save(hospitalUser);

        hospital = new Hospital();
        hospital.setUser(hospitalUser);
        hospital.setHospitalName("City Emergency Hospital");
        hospital.setRegistrationNumber("HOSP-" + uid);
        hospital.setContactNumber("042-99887766");
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setCompleteAddress("100 Main Hospital Blvd");
        hospital.setVerified(true);
        hospital = hospitalRepository.save(hospital);

        // Donor A (O_POSITIVE, available, verified)
        donorUserA = new User();
        donorUserA.setEmail("donora-" + uid + "@gmail.com");
        donorUserA.setPhoneNumber("0300-3" + uid.substring(0, 6));
        donorUserA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        donorUserA.setFullName("Donor Alpha");
        donorUserA.setRole(Role.DONOR);
        donorUserA.setAccountStatus(AccountStatus.ACTIVE);
        donorUserA.setEnabled(true);
        donorUserA = userRepository.save(donorUserA);

        donorA = new Donor();
        donorA.setUser(donorUserA);
        donorA.setBloodGroup(BloodGroup.O_POSITIVE);
        donorA.setDateOfBirth(LocalDate.now().minusYears(25));
        donorA.setGender(Gender.MALE);
        donorA.setWeight(new BigDecimal("72.0"));
        donorA.setProvince(province);
        donorA.setCity(city);
        donorA.setCompleteAddress("12 Green St");
        donorA.setVerified(true);
        donorA.setAvailability(DonorAvailability.AVAILABLE);
        donorA = donorRepository.save(donorA);

        // Donor B (O_POSITIVE, available, verified)
        donorUserB = new User();
        donorUserB.setEmail("donorb-" + uid + "@gmail.com");
        donorUserB.setPhoneNumber("0300-4" + uid.substring(0, 6));
        donorUserB.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        donorUserB.setFullName("Donor Beta");
        donorUserB.setRole(Role.DONOR);
        donorUserB.setAccountStatus(AccountStatus.ACTIVE);
        donorUserB.setEnabled(true);
        donorUserB = userRepository.save(donorUserB);

        donorB = new Donor();
        donorB.setUser(donorUserB);
        donorB.setBloodGroup(BloodGroup.O_POSITIVE);
        donorB.setDateOfBirth(LocalDate.now().minusYears(28));
        donorB.setGender(Gender.FEMALE);
        donorB.setWeight(new BigDecimal("60.0"));
        donorB.setProvince(province);
        donorB.setCity(city);
        donorB.setCompleteAddress("45 Blue Ave");
        donorB.setVerified(true);
        donorB.setAvailability(DonorAvailability.AVAILABLE);
        donorB = donorRepository.save(donorB);
    }

    @Test
    @DisplayName("Complete End-to-End Workflow: Request creation, Admin approval, Matching, Multi-Donor acceptance, Messaging, Appointment scheduling, Clinical intake, Confirmation, and Dashboard synchronization")
    void fullBloodDonationLifecycleAndMessagingIntegration() {
        // Step 1: Hospital creates blood request for 2 units of O_POSITIVE
        BloodRequest request = new BloodRequest();
        request.setHospital(hospital);
        request.setBloodGroup(BloodGroup.O_POSITIVE);
        request.setUnitsRequired(2);
        request.setUrgency(RequestUrgency.CRITICAL);
        request.setProvince(province);
        request.setCity(city);
        request.setHospitalNotes("Emergency patient requires 2 units of O+ blood");
        request.setStatus(RequestStatus.PENDING_ADMIN_APPROVAL);
        request.setUnitsFulfilled(0);
        request = bloodRequestRepository.save(request);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING_ADMIN_APPROVAL);
        assertThat(request.getUnitsFulfilled()).isEqualTo(0);

        // Step 2: Admin approves blood request
        BloodRequest approved = adminBloodRequestService.approveBloodRequest(
                request.getId(), "Approved for urgent emergency search", adminUser.getEmail());
        assertThat(approved.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo(adminUser.getEmail());
        assertThat(approved.getApprovedAt()).isNotNull();

        // Step 3: Trigger Matching Algorithm
        var matchSummary = donorMatchingService.findAndSaveMatchesForRequest(approved.getId(), hospitalUser.getEmail());
        assertThat(matchSummary.getTotalMatchesFound()).isGreaterThanOrEqualTo(2);

        List<RequestMatch> matches = requestMatchRepository.findByBloodRequestId(approved.getId());
        assertThat(matches).hasSizeGreaterThanOrEqualTo(2);

        RequestMatch matchA = matches.stream().filter(m -> m.getDonor().getId().equals(donorA.getId())).findFirst().orElseThrow();
        RequestMatch matchB = matches.stream().filter(m -> m.getDonor().getId().equals(donorB.getId())).findFirst().orElseThrow();
        assertThat(matchA.getMatchStatus()).isIn(MatchStatus.PENDING, MatchStatus.INVITED);
        assertThat(matchB.getMatchStatus()).isIn(MatchStatus.PENDING, MatchStatus.INVITED);

        // Step 4: Donor A accepts match
        donorMatchingService.respondToMatch(matchA.getId(), donorUserA.getEmail(), true, "Ready to donate tomorrow at 10 AM");
        RequestMatch refreshedMatchA = requestMatchRepository.findById(matchA.getId()).orElseThrow();
        assertThat(refreshedMatchA.getMatchStatus()).isEqualTo(MatchStatus.ACCEPTED);

        // Verify conversation was automatically created between Donor A and Hospital
        List<ConversationSummaryDTO> donorAConversations = messagingService.getDonorConversations(donorUserA.getEmail());
        assertThat(donorAConversations).isNotEmpty();
        Long convIdA = donorAConversations.get(0).getId();

        // Step 5: Donor B accepts independently (multi-donor assignment integrity)
        donorMatchingService.respondToMatch(matchB.getId(), donorUserB.getEmail(), true, "Available immediately");
        RequestMatch refreshedMatchB = requestMatchRepository.findById(matchB.getId()).orElseThrow();
        assertThat(refreshedMatchB.getMatchStatus()).isEqualTo(MatchStatus.ACCEPTED);

        // Both donors must have distinct active assignments
        assertThat(refreshedMatchA.getId()).isNotEqualTo(refreshedMatchB.getId());

        // Step 6: Direct Messaging coordination
        // Donor A sends message
        MessageResponseDTO msg1 = messagingService.sendMessageAndReturnDTO(convIdA, "Hello Hospital, I can be there at 10 AM sharp.", donorUserA.getEmail(), null);
        assertThat(msg1.getContent()).contains("10 AM sharp");

        // Hospital sends reply to Donor A
        MessageResponseDTO msg2 = messagingService.sendMessageAndReturnDTO(convIdA, "Thank you Donor Alpha! Please report to Blood Bank Ward 2.", hospitalUser.getEmail(), null);
        assertThat(msg2.getContent()).contains("Ward 2");

        // Admin checks platform conversations and participates
        List<ConversationSummaryDTO> adminConvs = messagingService.getAdminConversations(adminUser.getEmail());
        assertThat(adminConvs).isNotEmpty();
        MessageResponseDTO adminMsg = messagingService.sendMessageAndReturnDTO(
                convIdA, "Admin notice: Please bring valid CNIC for verification.", adminUser.getEmail(), donorUserA.getId());
        assertThat(adminMsg.getContent()).contains("Admin notice");
        assertThat(adminMsg.getSenderRole()).isEqualTo("ADMIN");

        // Step 7: Hospital schedules Donor A for appointment
        LocalDateTime schedTime = LocalDateTime.now().plusHours(4);
        RequestMatch scheduledA = hospitalDonationService.scheduleMatchAppointment(
                hospitalUser.getEmail(), refreshedMatchA.getId(), schedTime, "Ward 2 Blood Bank Intake Desk");
        assertThat(scheduledA.getMatchStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(scheduledA.getScheduledAppointmentTime()).isEqualTo(schedTime);
        assertThat(scheduledA.getAppointmentNotes()).contains("Ward 2");

        // Donor B remains in ACCEPTED status without interference
        RequestMatch checkMatchB = requestMatchRepository.findById(refreshedMatchB.getId()).orElseThrow();
        assertThat(checkMatchB.getMatchStatus()).isEqualTo(MatchStatus.ACCEPTED);

        // Step 8: Donor A arrives at hospital
        RequestMatch arrivedA = hospitalDonationService.markMatchArrived(hospitalUser.getEmail(), scheduledA.getId());
        assertThat(arrivedA.getMatchStatus()).isEqualTo(MatchStatus.ARRIVED);

        RequestMatch inProgressA = hospitalDonationService.markMatchInProgress(hospitalUser.getEmail(), arrivedA.getId());
        assertThat(inProgressA.getMatchStatus()).isEqualTo(MatchStatus.DONATION_IN_PROGRESS);

        // Step 9: Hospital confirms donation from Donor A (1 unit collected)
        Donation donationA = hospitalDonationService.confirmMatchDonation(
                hospitalUser.getEmail(), inProgressA.getId(), 1, "Bag #DN-101 successful draw", true);

        assertThat(donationA.getDonationStatus()).isEqualTo(DonationStatus.COMPLETED);
        assertThat(donationA.getUnitsDonated()).isEqualTo(1);
        assertThat(donationA.getInventoryAdded()).isTrue();

        // Verify match status updated to CONFIRMED
        RequestMatch confirmedA = requestMatchRepository.findById(matchA.getId()).orElseThrow();
        assertThat(confirmedA.getMatchStatus()).isEqualTo(MatchStatus.CONFIRMED);
        assertThat(confirmedA.getUnitsDonated()).isEqualTo(1);

        // Verify donor A deferral cooldown updated
        Donor donorARefreshed = donorRepository.findById(donorA.getId()).orElseThrow();
        assertThat(donorARefreshed.getLastDonationDate()).isEqualTo(LocalDate.now());

        // Verify Blood Request is now PARTIALLY_FULFILLED with 1 / 2 units
        BloodRequest reqAfterA = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reqAfterA.getUnitsFulfilled()).isEqualTo(1);
        assertThat(reqAfterA.getStatus()).isEqualTo(RequestStatus.PARTIALLY_FULFILLED);

        // Step 10: Duplicate confirmation prevention on same match
        assertThatThrownBy(() -> hospitalDonationService.confirmMatchDonation(
                hospitalUser.getEmail(), matchA.getId(), 1, "Duplicate try", true))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("already been confirmed");

        // Step 11: Donor B arrives, donation in progress, and confirmed for 2nd unit
        hospitalDonationService.markMatchArrived(hospitalUser.getEmail(), matchB.getId());
        hospitalDonationService.markMatchInProgress(hospitalUser.getEmail(), matchB.getId());
        Donation donationB = hospitalDonationService.confirmMatchDonation(
                hospitalUser.getEmail(), matchB.getId(), 1, "Bag #DN-102 successful draw", true);

        assertThat(donationB.getDonationStatus()).isEqualTo(DonationStatus.COMPLETED);

        // Verify Blood Request is now FULFILLED with 2 / 2 units
        BloodRequest reqAfterB = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reqAfterB.getUnitsFulfilled()).isEqualTo(2);
        assertThat(reqAfterB.getStatus()).isEqualTo(RequestStatus.FULFILLED);

        // Step 12: Verify Hospital Dashboard Synchronization
        HospitalDashboardSummary hospSummary = hospitalService.getDashboardSummary(hospitalUser.getEmail());
        assertThat(hospSummary.getFulfilledRequestsCount()).isGreaterThanOrEqualTo(1);
        assertThat(hospSummary.getConfirmedUnitsFulfilled()).isGreaterThanOrEqualTo(2);
        assertThat(hospSummary.getTotalUnitsRequired()).isGreaterThanOrEqualTo(2);

        // Step 13: Verify Donor Dashboard Synchronization
        DonorDashboardSummary donorSummary = donorService.getDashboardSummary(donorUserA.getEmail());
        assertThat(donorSummary.getCompletedDonationsCount()).isGreaterThanOrEqualTo(1);
        assertThat(donorSummary.getTotalUnitsDonated()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Admin Rejection and No-Show Life Cycles")
    void adminRejectionAndNoShowLifecycles() {
        // Create request
        BloodRequest request = new BloodRequest();
        request.setHospital(hospital);
        request.setBloodGroup(BloodGroup.A_POSITIVE);
        request.setUnitsRequired(1);
        request.setUrgency(RequestUrgency.URGENT);
        request.setProvince(province);
        request.setCity(city);
        request.setStatus(RequestStatus.PENDING_ADMIN_APPROVAL);
        request = bloodRequestRepository.save(request);

        // Admin rejects request with reason
        BloodRequest rejected = adminBloodRequestService.rejectBloodRequest(
                request.getId(), "Incomplete medical protocol justification", adminUser.getEmail());
        assertThat(rejected.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(rejected.getAdminNotes()).contains("Incomplete medical protocol justification");

        // Create match and verify NO_SHOW lifecycle
        RequestMatch match = new RequestMatch();
        match.setBloodRequest(rejected);
        match.setDonor(donorA);
        match.setMatchScore(90);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match = requestMatchRepository.save(match);

        RequestMatch noShowMatch = hospitalDonationService.markMatchNoShow(
                hospitalUser.getEmail(), match.getId(), "Donor phone was unreachable at appointment time");
        assertThat(noShowMatch.getMatchStatus()).isEqualTo(MatchStatus.NO_SHOW);
        assertThat(noShowMatch.getAppointmentNotes()).contains("unreachable");
    }

    @Test
    @DisplayName("Unauthorized Access Denial: Hospital cannot confirm matches belonging to another facility")
    void multiTenantAccessControlOnDonationConfirmation() {
        String uid2 = UUID.randomUUID().toString().substring(0, 8);
        User hospitalUser2 = new User();
        hospitalUser2.setEmail("other-hosp-" + uid2 + "@donornexa.com");
        hospitalUser2.setPhoneNumber("0300-5" + uid2.substring(0, 6));
        hospitalUser2.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        hospitalUser2.setFullName("Other Hospital");
        hospitalUser2.setRole(Role.HOSPITAL);
        hospitalUser2.setAccountStatus(AccountStatus.ACTIVE);
        hospitalUser2.setEnabled(true);
        hospitalUser2 = userRepository.save(hospitalUser2);

        Hospital hospital2 = new Hospital();
        hospital2.setUser(hospitalUser2);
        hospital2.setHospitalName("Other Hospital");
        hospital2.setRegistrationNumber("HOSP-OTHER-" + uid2);
        hospital2.setProvince(province);
        hospital2.setCity(city);
        hospital2.setVerified(true);
        hospital2 = hospitalRepository.save(hospital2);

        BloodRequest req2 = new BloodRequest();
        req2.setHospital(hospital2);
        req2.setBloodGroup(BloodGroup.O_POSITIVE);
        req2.setUnitsRequired(1);
        req2.setUrgency(RequestUrgency.CRITICAL);
        req2.setProvince(province);
        req2.setCity(city);
        req2.setStatus(RequestStatus.APPROVED);
        req2 = bloodRequestRepository.save(req2);

        RequestMatch matchOther = new RequestMatch();
        matchOther.setBloodRequest(req2);
        matchOther.setDonor(donorA);
        matchOther.setMatchScore(85);
        matchOther.setMatchStatus(MatchStatus.ARRIVED);
        final RequestMatch savedMatchOther = requestMatchRepository.save(matchOther);

        // First hospital attempts to confirm donation on hospital2's match
        assertThatThrownBy(() -> hospitalDonationService.confirmMatchDonation(
                hospitalUser.getEmail(), savedMatchOther.getId(), 1, "Intruder attempt", true))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied: You do not own this blood request");
    }
}
