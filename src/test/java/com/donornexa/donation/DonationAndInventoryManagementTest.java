package com.donornexa.donation;

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
import com.donornexa.hospital.dto.DirectStockEntryDTO;
import com.donornexa.hospital.dto.IssueStockDTO;
import com.donornexa.hospital.dto.RecordDonationDTO;
import com.donornexa.hospital.dto.UpdateDonationStatusDTO;
import com.donornexa.common.exception.HospitalOperationException;
import com.donornexa.hospital.service.HospitalDonationService;
import com.donornexa.hospital.service.HospitalInventoryService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 8 â€” Comprehensive Donation Management & Blood Inventory Integration & Security Tests.
 * Verifies donation lifecycle, donor IDOR protection, hospital isolation, inventory intake,
 * FIFO earliest-expiry dispatch, expired stock exclusion, duplicate prevention, and request fulfillment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Phase 8 â€” Donation Management & Blood Inventory Tests")
class DonationAndInventoryManagementTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private DonationRepository donationRepository;
    @Autowired private BloodInventoryRepository bloodInventoryRepository;
    @Autowired private BloodStockEntryRepository bloodStockEntryRepository;
    @Autowired private BloodRequestRepository bloodRequestRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;

    @Autowired private HospitalDonationService hospitalDonationService;
    @Autowired private HospitalInventoryService hospitalInventoryService;

    private Hospital hospitalA;
    private Hospital hospitalB;
    private User hospUserA;
    private User hospUserB;

    private Donor donor1;
    private User donorUser1;
    private Donor donor2;
    private User donorUser2;

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

        // Hospital A
        hospUserA = new User();
        hospUserA.setEmail("hospital.alpha." + UUID.randomUUID() + "@donornexa.com");
        hospUserA.setPhoneNumber("0300111" + (System.currentTimeMillis() % 10000));
        hospUserA.setPasswordHash("hashed_pw");
        hospUserA.setRole(Role.HOSPITAL);
        hospUserA.setFullName("Alpha General Hospital");
        hospUserA.setAccountStatus(AccountStatus.ACTIVE);
        hospUserA = userRepository.save(hospUserA);

        hospitalA = new Hospital();
        hospitalA.setUser(hospUserA);
        hospitalA.setHospitalName("Alpha General Hospital");
        hospitalA.setRegistrationNumber("REG-ALPHA-" + UUID.randomUUID());
        hospitalA.setProvince(province);
        hospitalA.setCity(city);
        hospitalA.setCompleteAddress("Alpha Road, Karachi");
        hospitalA.setContactNumber("03001112233");
        hospitalA.setVerified(true);
        hospitalA = hospitalRepository.save(hospitalA);

        // Hospital B
        hospUserB = new User();
        hospUserB.setEmail("hospital.beta." + UUID.randomUUID() + "@donornexa.com");
        hospUserB.setPhoneNumber("0300222" + (System.currentTimeMillis() % 10000));
        hospUserB.setPasswordHash("hashed_pw");
        hospUserB.setRole(Role.HOSPITAL);
        hospUserB.setFullName("Beta Memorial Hospital");
        hospUserB.setAccountStatus(AccountStatus.ACTIVE);
        hospUserB = userRepository.save(hospUserB);

        hospitalB = new Hospital();
        hospitalB.setUser(hospUserB);
        hospitalB.setHospitalName("Beta Memorial Hospital");
        hospitalB.setRegistrationNumber("REG-BETA-" + UUID.randomUUID());
        hospitalB.setProvince(province);
        hospitalB.setCity(city);
        hospitalB.setCompleteAddress("Beta Boulevard, Karachi");
        hospitalB.setContactNumber("03004445566");
        hospitalB.setVerified(true);
        hospitalB = hospitalRepository.save(hospitalB);

        // Donor 1 (Verified, O_POSITIVE)
        donorUser1 = new User();
        donorUser1.setEmail("donor1." + UUID.randomUUID() + "@donornexa.com");
        donorUser1.setPhoneNumber("0312111" + (System.currentTimeMillis() % 10000));
        donorUser1.setPasswordHash("hashed_pw");
        donorUser1.setRole(Role.DONOR);
        donorUser1.setFullName("Ali Khan");
        donorUser1.setAccountStatus(AccountStatus.ACTIVE);
        donorUser1 = userRepository.save(donorUser1);

        donor1 = new Donor();
        donor1.setUser(donorUser1);
        donor1.setBloodGroup(BloodGroup.O_POSITIVE);
        donor1.setGender(Gender.MALE);
        donor1.setDateOfBirth(LocalDate.of(1995, 5, 20));
        donor1.setWeight(new BigDecimal("70.0"));
        donor1.setProvince(province);
        donor1.setCity(city);
        donor1.setCompleteAddress("Gulshan, Karachi");
        donor1.setVerified(true);
        donor1.setAvailability(DonorAvailability.AVAILABLE);
        donor1 = donorRepository.save(donor1);

        // Donor 2 (Pending verification, A_POSITIVE)
        donorUser2 = new User();
        donorUser2.setEmail("donor2." + UUID.randomUUID() + "@donornexa.com");
        donorUser2.setPhoneNumber("0312222" + (System.currentTimeMillis() % 10000));
        donorUser2.setPasswordHash("hashed_pw");
        donorUser2.setRole(Role.DONOR);
        donorUser2.setFullName("Usman Tariq");
        donorUser2.setAccountStatus(AccountStatus.ACTIVE);
        donorUser2 = userRepository.save(donorUser2);

        donor2 = new Donor();
        donor2.setUser(donorUser2);
        donor2.setBloodGroup(BloodGroup.A_POSITIVE);
        donor2.setGender(Gender.MALE);
        donor2.setDateOfBirth(LocalDate.of(1998, 8, 15));
        donor2.setWeight(new BigDecimal("68.0"));
        donor2.setProvince(province);
        donor2.setCity(city);
        donor2.setCompleteAddress("Clifton, Karachi");
        donor2.setVerified(false);
        donor2.setAvailability(DonorAvailability.AVAILABLE);
        donor2 = donorRepository.save(donor2);
    }

    @Test
    @DisplayName("Hospital records donation with verified donor - status SCHEDULED")
    void testRecordDonation_Success() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        dto.setNotes("Regular scheduled donation appointment");

        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        assertThat(donation).isNotNull();
        assertThat(donation.getId()).isNotNull();
        assertThat(donation.getHospital().getId()).isEqualTo(hospitalA.getId());
        assertThat(donation.getDonor().getId()).isEqualTo(donor1.getId());
        assertThat(donation.getDonationStatus()).isEqualTo(DonationStatus.SCHEDULED);
        assertThat(donation.getInventoryAdded()).isFalse();
        assertThat(donation.getUnitsDonated()).isEqualTo(1);
    }

    @Test
    @DisplayName("Hospital recording donation for unverified donor throws exception")
    void testRecordDonation_UnverifiedDonor_ThrowsException() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor2.getId()); // Pending verification
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());

        assertThatThrownBy(() -> hospitalDonationService.recordDonation(hospUserA.getEmail(), dto))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("verified donors");
    }

    @Test
    @DisplayName("Hospital updates donation status: SCHEDULED -> COMPLETED")
    void testUpdateDonationStatus_Completed() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        UpdateDonationStatusDTO updateDTO = new UpdateDonationStatusDTO();
        updateDTO.setStatus(DonationStatus.COMPLETED);
        updateDTO.setAddToInventory(false);
        updateDTO.setNotes("Phlebotomy successful, donor rested 15 mins");

        Donation updated = hospitalDonationService.updateDonationStatus(hospUserA.getEmail(), donation.getId(), updateDTO);

        assertThat(updated.getDonationStatus()).isEqualTo(DonationStatus.COMPLETED);
        // Donor last donation date updated
        Donor reloadedDonor = donorRepository.findById(donor1.getId()).orElseThrow();
        assertThat(reloadedDonor.getLastDonationDate()).isNotNull();
    }

    @Test
    @DisplayName("Hospital cannot modify another hospital's donation (IDOR isolation)")
    void testHospitalIsolation_DonationIDORProtection() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        UpdateDonationStatusDTO updateDTO = new UpdateDonationStatusDTO();
        updateDTO.setStatus(DonationStatus.COMPLETED);

        // Hospital B attempts to update Hospital A's donation
        assertThatThrownBy(() -> hospitalDonationService.updateDonationStatus(hospUserB.getEmail(), donation.getId(), updateDTO))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("Access Denied");
    }

    @Test
    @DisplayName("Completed donation can be added to blood inventory exactly once (duplicate prevention)")
    void testAddDonationToInventory_SuccessAndDuplicatePrevention() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        UpdateDonationStatusDTO updateDTO = new UpdateDonationStatusDTO();
        updateDTO.setStatus(DonationStatus.COMPLETED);
        updateDTO.setAddToInventory(false);
        hospitalDonationService.updateDonationStatus(hospUserA.getEmail(), donation.getId(), updateDTO);

        // First addition: Success
        Donation updatedDonation = hospitalDonationService.addDonationToInventory(hospUserA.getEmail(), donation.getId());
        assertThat(updatedDonation).isNotNull();
        assertThat(updatedDonation.getInventoryAdded()).isTrue();

        // Check created stock entry
        BloodStockEntry entry = bloodStockEntryRepository.findByDonationId(donation.getId()).orElseThrow();
        assertThat(entry.getBloodGroup()).isEqualTo(BloodGroup.O_POSITIVE);
        assertThat(entry.getUnits()).isEqualTo(1);
        assertThat(entry.getEntryType()).isEqualTo(StockEntryType.DONATION_INTAKE);
        assertThat(entry.getStatus()).isEqualTo(StockItemStatus.AVAILABLE);
        assertThat(entry.getExpiryDate()).isEqualTo(LocalDate.now().plusDays(35));

        // Duplicate addition throws exception
        assertThatThrownBy(() -> hospitalDonationService.addDonationToInventory(hospUserA.getEmail(), donation.getId()))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("already been");
    }

    @Test
    @DisplayName("Cannot add SCHEDULED donation to inventory")
    void testAddDonationToInventory_ScheduledStatus_ThrowsException() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        assertThatThrownBy(() -> hospitalDonationService.addDonationToInventory(hospUserA.getEmail(), donation.getId()))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("Only COMPLETED donations");
    }

    @Test
    @DisplayName("Direct stock intake creates batch and updates aggregated BloodInventory")
    void testDirectStockIntake_Success() {
        DirectStockEntryDTO dto = new DirectStockEntryDTO();
        dto.setBloodGroup(BloodGroup.B_POSITIVE);
        dto.setUnits(3);
        dto.setCollectionDate(LocalDate.now());
        dto.setShelfLifeDays(35);
        dto.setNotes("External donation drive intake");

        BloodStockEntry entry = hospitalInventoryService.addDirectStock(hospUserA.getEmail(), dto);

        assertThat(entry).isNotNull();
        assertThat(entry.getBloodGroup()).isEqualTo(BloodGroup.B_POSITIVE);
        assertThat(entry.getUnits()).isEqualTo(3);
        assertThat(entry.getEntryType()).isEqualTo(StockEntryType.DIRECT_INTAKE);

        // Aggregated BloodInventory has 3 units
        BloodInventory inv = bloodInventoryRepository.findByHospitalIdAndBloodGroup(hospitalA.getId(), BloodGroup.B_POSITIVE).orElseThrow();
        assertThat(inv.getAvailableUnits()).isEqualTo(3);
        assertThat(inv.getInventoryStatus()).isEqualTo(InventoryStatus.AVAILABLE);
    }

    @Test
    @DisplayName("FIFO issuance: Earliest-expiring batch is issued first")
    void testIssueStock_FifoEarliestExpiryDeduction() {
        // Batch 1: Expires in 10 days, 2 units
        BloodStockEntry batch1 = new BloodStockEntry();
        batch1.setHospital(hospitalA);
        batch1.setBloodGroup(BloodGroup.A_POSITIVE);
        batch1.setUnits(2);
        batch1.setCollectionDate(LocalDate.now().minusDays(25));
        batch1.setExpiryDate(LocalDate.now().plusDays(10));
        batch1.setEntryType(StockEntryType.DIRECT_INTAKE);
        batch1.setStatus(StockItemStatus.AVAILABLE);
        bloodStockEntryRepository.save(batch1);

        // Batch 2: Expires in 30 days, 3 units
        BloodStockEntry batch2 = new BloodStockEntry();
        batch2.setHospital(hospitalA);
        batch2.setBloodGroup(BloodGroup.A_POSITIVE);
        batch2.setUnits(3);
        batch2.setCollectionDate(LocalDate.now().minusDays(5));
        batch2.setExpiryDate(LocalDate.now().plusDays(30));
        batch2.setEntryType(StockEntryType.DIRECT_INTAKE);
        batch2.setStatus(StockItemStatus.AVAILABLE);
        bloodStockEntryRepository.save(batch2);

        // Sync aggregate
        hospitalInventoryService.syncAggregateInventory(hospitalA, BloodGroup.A_POSITIVE);

        // Total available is 5 units. Now issue 3 units.
        IssueStockDTO issueDTO = new IssueStockDTO();
        issueDTO.setBloodGroup(BloodGroup.A_POSITIVE);
        issueDTO.setUnits(3);
        issueDTO.setNotes("Emergency transfusion surgery");

        hospitalInventoryService.issueStock(hospUserA.getEmail(), issueDTO);

        // Verify Batch 1 (earliest expiry) was fully consumed: status=ISSUED
        BloodStockEntry reloaded1 = bloodStockEntryRepository.findById(batch1.getId()).orElseThrow();
        assertThat(reloaded1.getStatus()).isEqualTo(StockItemStatus.ISSUED);

        // Verify Batch 2 had 1 unit deducted: units=2, status=AVAILABLE
        BloodStockEntry reloaded2 = bloodStockEntryRepository.findById(batch2.getId()).orElseThrow();
        assertThat(reloaded2.getUnits()).isEqualTo(2);
        assertThat(reloaded2.getStatus()).isEqualTo(StockItemStatus.AVAILABLE);

        // Aggregated units available is now 2
        BloodInventory inv = bloodInventoryRepository.findByHospitalIdAndBloodGroup(hospitalA.getId(), BloodGroup.A_POSITIVE).orElseThrow();
        assertThat(inv.getAvailableUnits()).isEqualTo(2);
    }

    @Test
    @DisplayName("Stock issuance fails with insufficient stock exception and avoids negative quantity")
    void testIssueStock_InsufficientStock_ThrowsException() {
        DirectStockEntryDTO dto = new DirectStockEntryDTO();
        dto.setBloodGroup(BloodGroup.AB_NEGATIVE);
        dto.setUnits(1);
        dto.setCollectionDate(LocalDate.now());
        dto.setShelfLifeDays(35);
        hospitalInventoryService.addDirectStock(hospUserA.getEmail(), dto);

        IssueStockDTO issueDTO = new IssueStockDTO();
        issueDTO.setBloodGroup(BloodGroup.AB_NEGATIVE);
        issueDTO.setUnits(5); // More than available

        assertThatThrownBy(() -> hospitalInventoryService.issueStock(hospUserA.getEmail(), issueDTO))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("Insufficient unexpired stock");

        // Verify quantity remains untouched
        BloodInventory inv = bloodInventoryRepository.findByHospitalIdAndBloodGroup(hospitalA.getId(), BloodGroup.AB_NEGATIVE).orElseThrow();
        assertThat(inv.getAvailableUnits()).isEqualTo(1);
    }

    @Test
    @DisplayName("Expired stock is excluded from issuance and available count")
    void testIssueStock_ExpiredStockExcluded() {
        // Expired batch: expired 2 days ago
        BloodStockEntry expiredBatch = new BloodStockEntry();
        expiredBatch.setHospital(hospitalA);
        expiredBatch.setBloodGroup(BloodGroup.O_NEGATIVE);
        expiredBatch.setUnits(4);
        expiredBatch.setCollectionDate(LocalDate.now().minusDays(37));
        expiredBatch.setExpiryDate(LocalDate.now().minusDays(2));
        expiredBatch.setEntryType(StockEntryType.DIRECT_INTAKE);
        expiredBatch.setStatus(StockItemStatus.AVAILABLE);
        bloodStockEntryRepository.save(expiredBatch);

        // Valid batch: expires in 20 days, only 1 unit
        BloodStockEntry validBatch = new BloodStockEntry();
        validBatch.setHospital(hospitalA);
        validBatch.setBloodGroup(BloodGroup.O_NEGATIVE);
        validBatch.setUnits(1);
        validBatch.setCollectionDate(LocalDate.now().minusDays(15));
        validBatch.setExpiryDate(LocalDate.now().plusDays(20));
        validBatch.setEntryType(StockEntryType.DIRECT_INTAKE);
        validBatch.setStatus(StockItemStatus.AVAILABLE);
        bloodStockEntryRepository.save(validBatch);

        hospitalInventoryService.syncAggregateInventory(hospitalA, BloodGroup.O_NEGATIVE);

        // Only 1 unit should be considered available, not 5
        IssueStockDTO issueDTO = new IssueStockDTO();
        issueDTO.setBloodGroup(BloodGroup.O_NEGATIVE);
        issueDTO.setUnits(2);

        assertThatThrownBy(() -> hospitalInventoryService.issueStock(hospUserA.getEmail(), issueDTO))
                .isInstanceOf(HospitalOperationException.class)
                .hasMessageContaining("Insufficient unexpired stock");
    }

    @Test
    @DisplayName("Stock issuance against blood request updates fulfilled units and auto-completes request")
    void testIssueStock_FulfillsBloodRequest() {
        // Create active blood request
        BloodRequest request = new BloodRequest();
        request.setHospital(hospitalA);
        request.setBloodGroup(BloodGroup.A_NEGATIVE);
        request.setUnitsRequired(2);
        request.setUnitsFulfilled(0);
        request.setUrgency(RequestUrgency.CRITICAL);
        request.setStatus(RequestStatus.MATCHING);
        request.setProvince(province);
        request.setCity(city);
        request.setRequiredBy(LocalDateTime.now().plusHours(6));
        request.setHospitalNotes("Emergency theater need");
        request = bloodRequestRepository.save(request);

        // Add 2 units stock
        DirectStockEntryDTO dto = new DirectStockEntryDTO();
        dto.setBloodGroup(BloodGroup.A_NEGATIVE);
        dto.setUnits(2);
        dto.setCollectionDate(LocalDate.now());
        dto.setShelfLifeDays(35);
        hospitalInventoryService.addDirectStock(hospUserA.getEmail(), dto);

        // Issue stock against request
        IssueStockDTO issueDTO = new IssueStockDTO();
        issueDTO.setBloodGroup(BloodGroup.A_NEGATIVE);
        issueDTO.setUnits(2);
        issueDTO.setBloodRequestId(request.getId());
        issueDTO.setNotes("Issued directly to emergency theater");

        hospitalInventoryService.issueStock(hospUserA.getEmail(), issueDTO);

        // Blood request is now FULFILLED
        BloodRequest reloadedRequest = bloodRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloadedRequest.getUnitsFulfilled()).isEqualTo(2);
        assertThat(reloadedRequest.getStatus()).isEqualTo(RequestStatus.FULFILLED);
    }

    @Test
    @DisplayName("Discard expired stock marks expired items as DISCARDED")
    void testDiscardExpiredStock_Success() {
        BloodStockEntry expiredBatch = new BloodStockEntry();
        expiredBatch.setHospital(hospitalA);
        expiredBatch.setBloodGroup(BloodGroup.B_NEGATIVE);
        expiredBatch.setUnits(3);
        expiredBatch.setCollectionDate(LocalDate.now().minusDays(40));
        expiredBatch.setExpiryDate(LocalDate.now().minusDays(5));
        expiredBatch.setEntryType(StockEntryType.DIRECT_INTAKE);
        expiredBatch.setStatus(StockItemStatus.AVAILABLE);
        bloodStockEntryRepository.save(expiredBatch);

        int discarded = hospitalInventoryService.discardExpiredStock(hospUserA.getEmail());
        assertThat(discarded).isGreaterThanOrEqualTo(1);

        BloodStockEntry reloaded = bloodStockEntryRepository.findById(expiredBatch.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(StockItemStatus.DISCARDED);
    }

    @Test
    @WithMockUser(username = "donor.user@donornexa.com", roles = "DONOR")
    @DisplayName("Donor can view own donation certificate but cannot access another donor's record (IDOR)")
    void testDonorDonationDetails_IDORProtection() throws Exception {
        // Create donation for donor 1
        Donation donation = new Donation();
        donation.setHospital(hospitalA);
        donation.setDonor(donor1);
        donation.setUnitsDonated(1);
        donation.setDonationDate(LocalDateTime.now());
        donation.setDonationStatus(DonationStatus.COMPLETED);
        donation.setInventoryAdded(true);
        donation = donationRepository.save(donation);

        // When donor1 accesses own donation: 200 OK
        mockMvc.perform(get("/donor/donations/" + donation.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser1.getEmail()).roles("DONOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("donor/donation-details"))
                .andExpect(model().attributeExists("donation"));

        // When donor2 accesses donor1's donation: Friendly IDOR protection redirects to donation ledger with error message
        mockMvc.perform(get("/donor/donations/" + donation.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(donorUser2.getEmail()).roles("DONOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/donations"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "hospital.user@donornexa.com", roles = "HOSPITAL")
    @DisplayName("Hospital dashboard and inventory web endpoints return 200 OK")
    void testHospitalInventoryWebEndpoints() throws Exception {
        mockMvc.perform(get("/hospital/inventory")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospUserA.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/inventory"))
                .andExpect(model().attributeExists("summary", "bloodGroups"));

        mockMvc.perform(get("/hospital/donations")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(hospUserA.getEmail()).roles("HOSPITAL")))
                .andExpect(status().isOk())
                .andExpect(view().name("hospital/donations"))
                .andExpect(model().attributeExists("donationPage"));
    }

    @Test
    @DisplayName("Hospital updates donation status: SCHEDULED -> CANCELLED")
    void testUpdateDonationStatus_Cancelled() {
        RecordDonationDTO dto = new RecordDonationDTO();
        dto.setDonorId(donor1.getId());
        dto.setUnitsDonated(1);
        dto.setDonationDate(LocalDateTime.now());
        dto.setDonationStatus(DonationStatus.SCHEDULED);
        dto.setAddToInventory(false);
        Donation donation = hospitalDonationService.recordDonation(hospUserA.getEmail(), dto);

        UpdateDonationStatusDTO updateDTO = new UpdateDonationStatusDTO();
        updateDTO.setStatus(DonationStatus.CANCELLED);
        updateDTO.setAddToInventory(false);
        updateDTO.setNotes("Donor had temporary low hemoglobin on screening");

        Donation updated = hospitalDonationService.updateDonationStatus(hospUserA.getEmail(), donation.getId(), updateDTO);

        assertThat(updated.getDonationStatus()).isEqualTo(DonationStatus.CANCELLED);
        assertThat(updated.getInventoryAdded()).isFalse();
    }

    @Test
    @WithMockUser(username = "admin@donornexa.com", roles = "ADMIN")
    @DisplayName("Administrator can access platform-wide donations and inventory monitoring endpoints")
    void testAdminAccess_DonationsAndInventory_Success() throws Exception {
        mockMvc.perform(get("/admin/donations"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/donations"))
                .andExpect(model().attributeExists("donationPage", "statuses"));

        mockMvc.perform(get("/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/inventory"))
                .andExpect(model().attributeExists("inventoryList", "totalUnits", "unitsByGroup"));
    }

    @Test
    @WithMockUser(username = "donor.test@donornexa.com", roles = "DONOR")
    @DisplayName("Unauthorized role: Donor cannot access hospital inventory endpoints (403 Forbidden)")
    void testDonorAccess_HospitalInventory_Forbidden() throws Exception {
        mockMvc.perform(get("/hospital/inventory"))
                .andExpect(status().isForbidden());
    }
}
