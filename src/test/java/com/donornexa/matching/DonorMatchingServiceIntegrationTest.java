package com.donornexa.matching;

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
import com.donornexa.matching.dto.DonorMatchDTO;
import com.donornexa.matching.dto.MatchingEvaluationSummaryDTO;
import com.donornexa.matching.service.DonorMatchingService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
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
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 7 â€” Donor Matching Service Integration Tests")
class DonorMatchingServiceIntegrationTest {

    @Autowired private DonorMatchingService matchingService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;
    @Autowired private NotificationRepository notificationRepository;

    private Province punjab;
    private Province sindh;
    private City lahore;
    private City rawalpindi;
    private City karachi;
    private Hospital testHospital;
    private User hospitalUser;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        punjab = provinceRepository.save(createProvince("Punjab-" + uid));
        sindh = provinceRepository.save(createProvince("Sindh-" + uid));

        lahore = cityRepository.save(createCity("Lahore-" + uid, punjab));
        rawalpindi = cityRepository.save(createCity("Rawalpindi-" + uid, punjab));
        karachi = cityRepository.save(createCity("Karachi-" + uid, sindh));

        hospitalUser = userRepository.save(createUser("hosp." + uid + "@donornexa.com", Role.HOSPITAL));
        testHospital = new Hospital();
        testHospital.setUser(hospitalUser);
        testHospital.setHospitalName("Shaukat Khanum Hospital " + uid);
        testHospital.setRegistrationNumber("HOSP-" + uid);
        testHospital.setProvince(punjab);
        testHospital.setCity(lahore);
        testHospital.setVerified(true);
        testHospital = hospitalRepository.save(testHospital);
    }

    private Province createProvince(String name) {
        Province p = new Province();
        p.setName(name);
        return p;
    }

    private City createCity(String name, Province p) {
        City c = new City();
        c.setName(name);
        c.setProvince(p);
        return c;
    }

    private User createUser(String email, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setFullName("User " + email);
        u.setPhoneNumber("0300" + (int)(Math.random() * 8999999 + 1000000));
        u.setPasswordHash("$2a$10$dummyHashGeneratedForTestPurposesOnly12345");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setEnabled(true);
        return u;
    }

    private Donor createDonor(String email, BloodGroup bg, City city, boolean verified,
                              DonorAvailability availability, LocalDate lastDonation) {
        User u = userRepository.save(createUser(email, Role.DONOR));
        Donor d = new Donor();
        d.setUser(u);
        d.setBloodGroup(bg);
        d.setDateOfBirth(LocalDate.now().minusYears(25));
        d.setGender(Gender.MALE);
        d.setWeight(BigDecimal.valueOf(68.0));
        d.setProvince(city.getProvince());
        d.setCity(city);
        d.setAvailability(availability);
        d.setVerified(verified);
        d.setLastDonationDate(lastDonation);
        return donorRepository.save(d);
    }

    private BloodRequest createRequest(BloodGroup bg, RequestUrgency urgency) {
        BloodRequest req = new BloodRequest();
        req.setHospital(testHospital);
        req.setBloodGroup(bg);
        req.setUnitsRequired(2);
        req.setUnitsFulfilled(0);
        req.setUrgency(urgency);
        req.setStatus(RequestStatus.PENDING);
        req.setProvince(testHospital.getProvince());
        req.setCity(testHospital.getCity());
        req.setRequiredBy(LocalDateTime.now().plusDays(2));
        return bloodRequestRepository.save(req);
    }

    // =========================================================================
    // 1. Basic Matching & Inclusion Tests
    // =========================================================================

    @Test
    @DisplayName("Matching includes compatible, verified, eligible donor and sets status to MATCHING")
    void matching_includesCompatibleDonor() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor donor = createDonor("donor." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getTotalMatchesFound()).isGreaterThanOrEqualTo(1);
        List<DonorMatchDTO> matches = summary.getMatches();
        assertThat(matches).anyMatch(m -> m.getDonorId().equals(donor.getId()));

        // Verify request transitioned to MATCHING
        BloodRequest reloaded = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(RequestStatus.MATCHING);
    }

    // =========================================================================
    // 2. Exclusion Matrix Tests
    // =========================================================================

    @Test
    @DisplayName("Incompatible blood group donor is strictly excluded")
    void matching_excludesIncompatibleDonor() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        // Recipient needs O-, only O- can donate. Donor is B+
        Donor incompatible = createDonor("incompat." + uid + "@test.com", BloodGroup.B_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getMatches()).noneMatch(m -> m.getDonorId().equals(incompatible.getId()));
    }

    @Test
    @DisplayName("Unverified donor is excluded from recommendations")
    void matching_excludesUnverifiedDonor() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor unverified = createDonor("unverified." + uid + "@test.com", BloodGroup.O_NEGATIVE, lahore, false,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getMatches()).noneMatch(m -> m.getDonorId().equals(unverified.getId()));
    }

    @Test
    @DisplayName("Unavailable donor is excluded from recommendations")
    void matching_excludesUnavailableDonor() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor unavailable = createDonor("unavailable." + uid + "@test.com", BloodGroup.O_NEGATIVE, lahore, true,
                DonorAvailability.UNAVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getMatches()).noneMatch(m -> m.getDonorId().equals(unavailable.getId()));
    }

    @Test
    @DisplayName("Donor within 90-day cooldown interval is excluded")
    void matching_excludesDonorInCooldown() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        // Donated 30 days ago (requires 90 days)
        Donor inCooldown = createDonor("cooldown." + uid + "@test.com", BloodGroup.O_NEGATIVE, lahore, true,
                DonorAvailability.AVAILABLE, LocalDate.now().minusDays(30));

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getMatches()).noneMatch(m -> m.getDonorId().equals(inCooldown.getId()));
    }

    @Test
    @DisplayName("Donor outside 90-day cooldown interval is included")
    void matching_includesDonorOutsideCooldown() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        // Donated 100 days ago (past 90-day cooldown)
        Donor ready = createDonor("ready." + uid + "@test.com", BloodGroup.O_NEGATIVE, lahore, true,
                DonorAvailability.AVAILABLE, LocalDate.now().minusDays(100));

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        assertThat(summary.getMatches()).anyMatch(m -> m.getDonorId().equals(ready.getId()));
    }

    // =========================================================================
    // 3. Proximity Ranking and Scoring
    // =========================================================================

    @Test
    @DisplayName("Same-city donor receives higher match score than same-province donor")
    void matching_sameCityRankedHigher() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor sameCity = createDonor("samecity." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);
        Donor otherCity = createDonor("othercity." + uid + "@test.com", BloodGroup.A_POSITIVE, rawalpindi, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);

        MatchingEvaluationSummaryDTO summary = matchingService.findAndSaveMatchesForRequest(
                request.getId(), hospitalUser.getEmail());

        DonorMatchDTO sameCityMatch = summary.getMatches().stream()
                .filter(m -> m.getDonorId().equals(sameCity.getId())).findFirst().orElseThrow();
        DonorMatchDTO otherCityMatch = summary.getMatches().stream()
                .filter(m -> m.getDonorId().equals(otherCity.getId())).findFirst().orElseThrow();

        assertThat(sameCityMatch.getMatchScore()).isGreaterThan(otherCityMatch.getMatchScore());
        assertThat(sameCityMatch.isSameCity()).isTrue();
        assertThat(otherCityMatch.isSameCity()).isFalse();
        assertThat(otherCityMatch.isSameProvince()).isTrue();
    }

    // =========================================================================
    // 4. Idempotency & Re-running
    // =========================================================================

    @Test
    @DisplayName("Re-running matching engine is idempotent: does not duplicate matches")
    void matching_rerunIsIdempotent() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor donor = createDonor("idemp." + uid + "@test.com", BloodGroup.O_NEGATIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.O_NEGATIVE, RequestUrgency.NORMAL);

        // Run 1
        matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail());
        long countAfterRun1 = requestMatchRepository.countByBloodRequestId(request.getId());

        // Run 2
        matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail());
        long countAfterRun2 = requestMatchRepository.countByBloodRequestId(request.getId());

        assertThat(countAfterRun1).isEqualTo(countAfterRun2);
    }

    // =========================================================================
    // 5. Donor Response Workflow
    // =========================================================================

    @Test
    @DisplayName("Donor can accept match, updates status and records timestamp")
    void donor_acceptMatchWorkflow() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor donor = createDonor("responder." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);
        matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail());

        RequestMatch match = requestMatchRepository.findByDonorIdAndBloodRequestId(donor.getId(), request.getId()).orElseThrow();
        assertThat(match.getMatchStatus()).isEqualTo(MatchStatus.PENDING);

        // Accept
        DonorMatchDTO updated = matchingService.respondToMatch(match.getId(), donor.getUser().getEmail(),
                true, "I can donate tomorrow at 2 PM");

        assertThat(updated.getMatchStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(updated.getResponseAt()).isNotNull();
        assertThat(updated.getDonorNotes()).isEqualTo("I can donate tomorrow at 2 PM");
    }

    @Test
    @DisplayName("Donor can decline match, updates status and records reason")
    void donor_declineMatchWorkflow() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor donor = createDonor("decliner." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);
        matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail());

        RequestMatch match = requestMatchRepository.findByDonorIdAndBloodRequestId(donor.getId(), request.getId()).orElseThrow();

        // Decline
        DonorMatchDTO updated = matchingService.respondToMatch(match.getId(), donor.getUser().getEmail(),
                false, "Traveling abroad");

        assertThat(updated.getMatchStatus()).isEqualTo(MatchStatus.REJECTED);
        assertThat(updated.getResponseAt()).isNotNull();
    }

    @Test
    @DisplayName("Donor cannot respond to another donor's match (IDOR Protection)")
    void donor_cannotRespondToAnotherDonorsMatch() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Donor donor1 = createDonor("victim." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);
        Donor donor2 = createDonor("attacker." + uid + "@test.com", BloodGroup.A_POSITIVE, lahore, true,
                DonorAvailability.AVAILABLE, null);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);
        matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail());

        RequestMatch match1 = requestMatchRepository.findByDonorIdAndBloodRequestId(donor1.getId(), request.getId()).orElseThrow();

        // Attacker attempts to respond to victim's match
        assertThatThrownBy(() -> matchingService.respondToMatch(match1.getId(), donor2.getUser().getEmail(), true, "Hacked"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    // =========================================================================
    // 6. Security & Authorization Checks
    // =========================================================================

    @Test
    @DisplayName("Hospital cannot run matching on another hospital's request")
    void hospital_cannotRunMatchingOnOtherHospitalRequest() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User otherHospitalUser = userRepository.save(createUser("otherhosp." + uid + "@test.com", Role.HOSPITAL));
        Hospital otherHospital = new Hospital();
        otherHospital.setUser(otherHospitalUser);
        otherHospital.setHospitalName("Other Hospital");
        otherHospital.setRegistrationNumber("HOSP-OTHER-" + uid);
        otherHospital.setProvince(punjab);
        otherHospital.setCity(lahore);
        otherHospital.setVerified(true);
        hospitalRepository.save(otherHospital);

        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);

        assertThatThrownBy(() -> matchingService.findAndSaveMatchesForRequest(request.getId(), otherHospitalUser.getEmail()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Matching cannot be executed on fulfilled or cancelled requests")
    void matching_cannotExecuteOnFulfilledOrCancelledRequest() {
        BloodRequest request = createRequest(BloodGroup.A_POSITIVE, RequestUrgency.NORMAL);
        request.setStatus(RequestStatus.FULFILLED);
        bloodRequestRepository.save(request);

        assertThatThrownBy(() -> matchingService.findAndSaveMatchesForRequest(request.getId(), hospitalUser.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FULFILLED");
    }
}
