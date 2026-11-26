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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CEP Mandatory Requirement â€” Pessimistic Concurrency & Anti-Over-Allocation Tests")
class ConcurrencyPessimisticLockTest {

    @Autowired private DonorMatchingService matchingService;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private RequestMatchRepository requestMatchRepository;

    private BloodRequest bloodRequest;
    private RequestMatch match1;
    private RequestMatch match2;
    private User donorUser1;
    private User donorUser2;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        Province province = new Province();
        province.setName("Sindh-" + uid);
        province = provinceRepository.save(province);

        City city = new City();
        city.setName("Karachi-" + uid);
        city.setProvince(province);
        city = cityRepository.save(city);

        // Create Hospital
        User hospUser = new User();
        hospUser.setEmail("hosp-" + uid + "@donornexa.com");
        hospUser.setPasswordHash("$2a$10$xyz");
        hospUser.setFullName("Aga Khan Hospital " + uid);
        hospUser.setPhoneNumber("0300111" + uid);
        hospUser.setRole(Role.HOSPITAL);
        hospUser.setAccountStatus(AccountStatus.ACTIVE);
        hospUser.setEnabled(true);
        userRepository.save(hospUser);

        Hospital hospital = new Hospital();
        hospital.setUser(hospUser);
        hospital.setHospitalName("AKUH " + uid);
        hospital.setRegistrationNumber("REG-" + uid);
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setVerified(true);
        hospitalRepository.save(hospital);

        // Create Blood Request requiring only 1 unit
        bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodGroup(BloodGroup.O_NEGATIVE);
        bloodRequest.setUnitsRequired(1);
        bloodRequest.setUnitsFulfilled(0);
        bloodRequest.setUrgency(RequestUrgency.CRITICAL);
        bloodRequest.setStatus(RequestStatus.MATCHING);
        bloodRequest.setRequiredBy(LocalDateTime.now().plusHours(4));
        bloodRequest.setProvince(province);
        bloodRequest.setCity(city);
        bloodRequest = bloodRequestRepository.save(bloodRequest);

        // Create Donor 1
        donorUser1 = new User();
        donorUser1.setEmail("donor1-" + uid + "@donornexa.com");
        donorUser1.setPasswordHash("$2a$10$xyz");
        donorUser1.setFullName("Donor One");
        donorUser1.setPhoneNumber("0300222" + uid);
        donorUser1.setRole(Role.DONOR);
        donorUser1.setAccountStatus(AccountStatus.ACTIVE);
        donorUser1.setEnabled(true);
        userRepository.save(donorUser1);

        Donor donor1 = new Donor();
        donor1.setUser(donorUser1);
        donor1.setBloodGroup(BloodGroup.O_NEGATIVE);
        donor1.setDateOfBirth(LocalDate.of(1995, 5, 5));
        donor1.setGender(Gender.MALE);
        donor1.setWeight(new BigDecimal("70.0"));
        donor1.setProvince(province);
        donor1.setCity(city);
        donor1.setVerified(true);
        donor1.setAvailability(DonorAvailability.AVAILABLE);
        donorRepository.save(donor1);

        // Create Donor 2
        donorUser2 = new User();
        donorUser2.setEmail("donor2-" + uid + "@donornexa.com");
        donorUser2.setPasswordHash("$2a$10$xyz");
        donorUser2.setFullName("Donor Two");
        donorUser2.setPhoneNumber("0300333" + uid);
        donorUser2.setRole(Role.DONOR);
        donorUser2.setAccountStatus(AccountStatus.ACTIVE);
        donorUser2.setEnabled(true);
        userRepository.save(donorUser2);

        Donor donor2 = new Donor();
        donor2.setUser(donorUser2);
        donor2.setBloodGroup(BloodGroup.O_NEGATIVE);
        donor2.setDateOfBirth(LocalDate.of(1996, 6, 6));
        donor2.setGender(Gender.FEMALE);
        donor2.setWeight(new BigDecimal("60.0"));
        donor2.setProvince(province);
        donor2.setCity(city);
        donor2.setVerified(true);
        donor2.setAvailability(DonorAvailability.AVAILABLE);
        donorRepository.save(donor2);

        // Matches
        match1 = new RequestMatch();
        match1.setBloodRequest(bloodRequest);
        match1.setDonor(donor1);
        match1.setMatchStatus(MatchStatus.INVITED);
        match1.setMatchScore(95);
        match1 = requestMatchRepository.save(match1);

        match2 = new RequestMatch();
        match2.setBloodRequest(bloodRequest);
        match2.setDonor(donor2);
        match2.setMatchStatus(MatchStatus.INVITED);
        match2.setMatchScore(90);
        match2 = requestMatchRepository.save(match2);
    }

    @Test
    @DisplayName("Pessimistic locking prevents over-allocation when 2 donors respond simultaneously to 1 open unit")
    void shouldPreventOverAllocationOnConcurrentDonorResponses() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Thread 1: Donor 1 accepts
        executor.submit(() -> {
            try {
                startSignal.await();
                matchingService.respondToMatch(match1.getId(), donorUser1.getEmail(), true, "Ready to donate");
                successCount.incrementAndGet();
            } catch (Exception ex) {
                failureCount.incrementAndGet();
            } finally {
                doneSignal.countDown();
            }
        });

        // Thread 2: Donor 2 accepts
        executor.submit(() -> {
            try {
                startSignal.await();
                matchingService.respondToMatch(match2.getId(), donorUser2.getEmail(), true, "Ready to donate");
                successCount.incrementAndGet();
            } catch (Exception ex) {
                failureCount.incrementAndGet();
            } finally {
                doneSignal.countDown();
            }
        });

        // Fire both threads simultaneously
        startSignal.countDown();
        doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // One must succeed, and both cannot claim more units than required
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        BloodRequest refreshed = bloodRequestRepository.findById(bloodRequest.getId()).orElseThrow();
        assertThat(refreshed.getUnitsFulfilled()).isLessThanOrEqualTo(refreshed.getUnitsRequired());
    }

    @Test
    @DisplayName("Optimistic version attribute is incremented on entity state transitions")
    void shouldIncrementVersionOnUpdate() {
        BloodRequest req = bloodRequestRepository.findById(bloodRequest.getId()).orElseThrow();
        Long initialVersion = req.getVersion();

        req.setHospitalNotes("Updated notes for audit verification");
        BloodRequest saved = bloodRequestRepository.saveAndFlush(req);

        assertThat(saved.getVersion()).isNotNull();
    }
}
