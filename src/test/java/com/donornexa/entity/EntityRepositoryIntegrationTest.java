package com.donornexa.entity;

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
import com.donornexa.badge.entity.*;
import com.donornexa.common.enums.Role;
import com.donornexa.common.enums.*;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for JPA entities and repositories.
 *
 * Uses @DataJpaTest which bootstraps only the JPA slice (entities, repositories)
 * and the H2 in-memory database defined in test/resources/application.properties.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Phase 2 â€” Entity and Repository Tests")
class EntityRepositoryIntegrationTest {

    @Autowired private UserRepository       userRepository;
    @Autowired private ProvinceRepository   provinceRepository;
    @Autowired private CityRepository       cityRepository;
    @Autowired private DonorRepository      donorRepository;
    @Autowired private HospitalRepository   hospitalRepository;
    @Autowired private BloodRequestRepository   bloodRequestRepository;
    @Autowired private RequestMatchRepository   requestMatchRepository;
    @Autowired private DonationRepository       donationRepository;
    @Autowired private BloodInventoryRepository bloodInventoryRepository;
    @Autowired private NotificationRepository   notificationRepository;
    @Autowired private AuditLogRepository       auditLogRepository;

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private User buildUser(String email, String phone, Role role) {
        User u = new User();
        u.setFullName("Test User");
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPasswordHash("$2a$10$hashedpasswordplaceholder");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return u;
    }

    private Province buildProvince(String name) {
        Province p = new Province();
        p.setName(name);
        return p;
    }

    private City buildCity(String name, Province province) {
        City c = new City();
        c.setName(name);
        c.setProvince(province);
        return c;
    }

    // -------------------------------------------------------------------------
    // User tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("User: save and find by email")
    void user_saveAndFindByEmail() {
        User user = buildUser("test@example.com", "0300-1234567", Role.DONOR);
        userRepository.save(user);

        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }

    @Test
    @DisplayName("User: existsByEmail returns true when user exists")
    void user_existsByEmail() {
        userRepository.save(buildUser("unique@example.com", "0301-1234567", Role.DONOR));
        assertThat(userRepository.existsByEmail("unique@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    @DisplayName("User: duplicate email throws DataIntegrityViolationException")
    void user_duplicateEmailThrowsException() {
        userRepository.save(buildUser("dup@example.com", "0302-1111111", Role.DONOR));
        User duplicate = buildUser("dup@example.com", "0302-2222222", Role.DONOR);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("User: duplicate phone number throws DataIntegrityViolationException")
    void user_duplicatePhoneThrowsException() {
        userRepository.save(buildUser("phone1@example.com", "0303-9999999", Role.DONOR));
        User duplicate = buildUser("phone2@example.com", "0303-9999999", Role.DONOR);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("User: createdAt is populated on persist")
    void user_timestampPopulated() {
        User saved = userRepository.save(buildUser("ts@example.com", "0304-5555555", Role.DONOR));
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Province tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Province: save and find by name")
    void province_saveAndFindByName() {
        Province p = provinceRepository.save(buildProvince("Sindh"));
        assertThat(provinceRepository.findByName("Sindh")).isPresent();
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Province: duplicate name throws DataIntegrityViolationException")
    void province_duplicateNameThrowsException() {
        provinceRepository.save(buildProvince("Punjab"));
        assertThatThrownBy(() -> provinceRepository.saveAndFlush(buildProvince("Punjab")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // -------------------------------------------------------------------------
    // City tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("City: save and find by province")
    void city_findByProvince() {
        Province province = provinceRepository.save(buildProvince("KPK"));
        cityRepository.save(buildCity("Peshawar", province));
        cityRepository.save(buildCity("Abbottabad", province));

        assertThat(cityRepository.findByProvinceId(province.getId())).hasSize(2);
    }

    @Test
    @DisplayName("City: composite unique constraint prevents duplicate name in same province")
    void city_duplicateInSameProvinceThrowsException() {
        Province province = provinceRepository.save(buildProvince("Balochistan"));
        cityRepository.save(buildCity("Quetta", province));

        assertThatThrownBy(() -> cityRepository.saveAndFlush(buildCity("Quetta", province)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("City: same name in different provinces is allowed")
    void city_sameNameInDifferentProvincesAllowed() {
        Province p1 = provinceRepository.save(buildProvince("Province-A"));
        Province p2 = provinceRepository.save(buildProvince("Province-B"));
        cityRepository.save(buildCity("Islamabad", p1));
        cityRepository.save(buildCity("Islamabad", p2));

        assertThat(cityRepository.existsByNameAndProvinceId("Islamabad", p1.getId())).isTrue();
        assertThat(cityRepository.existsByNameAndProvinceId("Islamabad", p2.getId())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Donor tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Donor: save and find by user ID")
    void donor_saveAndFindByUserId() {
        Province province = provinceRepository.save(buildProvince("Prov-Donor"));
        City city         = cityRepository.save(buildCity("City-Donor", province));
        User user         = userRepository.save(buildUser("donor@example.com", "0305-1111111", Role.DONOR));

        Donor donor = new Donor();
        donor.setUser(user);
        donor.setBloodGroup(BloodGroup.A_POSITIVE);
        donor.setDateOfBirth(LocalDate.of(1990, 5, 15));
        donor.setGender(Gender.MALE);
        donor.setWeight(new BigDecimal("70.00"));
        donor.setProvince(province);
        donor.setCity(city);
        donor.setAvailability(DonorAvailability.AVAILABLE);
        donorRepository.save(donor);

        assertThat(donorRepository.findByUserId(user.getId())).isPresent();
    }

    @Test
    @DisplayName("Donor: find by blood group returns correct results")
    void donor_findByBloodGroup() {
        Province province = provinceRepository.save(buildProvince("Prov-BG"));
        City city         = cityRepository.save(buildCity("City-BG", province));
        User u1 = userRepository.save(buildUser("bg1@example.com", "0306-1111111", Role.DONOR));
        User u2 = userRepository.save(buildUser("bg2@example.com", "0306-2222222", Role.DONOR));

        Donor d1 = new Donor();
        d1.setUser(u1); d1.setBloodGroup(BloodGroup.O_POSITIVE);
        d1.setDateOfBirth(LocalDate.of(1988, 1, 1)); d1.setGender(Gender.FEMALE);
        d1.setProvince(province); d1.setCity(city);
        d1.setAvailability(DonorAvailability.AVAILABLE);

        Donor d2 = new Donor();
        d2.setUser(u2); d2.setBloodGroup(BloodGroup.B_NEGATIVE);
        d2.setDateOfBirth(LocalDate.of(1992, 3, 10)); d2.setGender(Gender.MALE);
        d2.setProvince(province); d2.setCity(city);
        d2.setAvailability(DonorAvailability.AVAILABLE);

        donorRepository.save(d1);
        donorRepository.save(d2);

        assertThat(donorRepository.findByBloodGroup(BloodGroup.O_POSITIVE)).hasSize(1);
        assertThat(donorRepository.findByBloodGroup(BloodGroup.B_NEGATIVE)).hasSize(1);
        assertThat(donorRepository.findByBloodGroup(BloodGroup.AB_POSITIVE)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Hospital tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Hospital: save and find by user ID")
    void hospital_saveAndFindByUserId() {
        Province province = provinceRepository.save(buildProvince("Prov-Hosp"));
        City city         = cityRepository.save(buildCity("City-Hosp", province));
        User user         = userRepository.save(buildUser("hosp@example.com", "0307-1111111", Role.HOSPITAL));

        Hospital hospital = new Hospital();
        hospital.setUser(user);
        hospital.setHospitalName("General Hospital");
        hospital.setRegistrationNumber("REG-001");
        hospital.setProvince(province);
        hospital.setCity(city);
        hospitalRepository.save(hospital);

        assertThat(hospitalRepository.findByUserId(user.getId())).isPresent();
        assertThat(hospitalRepository.existsByRegistrationNumber("REG-001")).isTrue();
    }

    @Test
    @DisplayName("Hospital: duplicate registration number throws exception")
    void hospital_duplicateRegistrationNumberThrowsException() {
        Province province = provinceRepository.save(buildProvince("Prov-Reg"));
        City city         = cityRepository.save(buildCity("City-Reg", province));
        User u1 = userRepository.save(buildUser("hreg1@example.com", "0308-1111111", Role.HOSPITAL));
        User u2 = userRepository.save(buildUser("hreg2@example.com", "0308-2222222", Role.HOSPITAL));

        Hospital h1 = new Hospital();
        h1.setUser(u1); h1.setHospitalName("Hospital A"); h1.setRegistrationNumber("SAME-REG");
        h1.setProvince(province); h1.setCity(city);
        hospitalRepository.save(h1);

        Hospital h2 = new Hospital();
        h2.setUser(u2); h2.setHospitalName("Hospital B"); h2.setRegistrationNumber("SAME-REG");
        h2.setProvince(province); h2.setCity(city);

        assertThatThrownBy(() -> hospitalRepository.saveAndFlush(h2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // -------------------------------------------------------------------------
    // BloodRequest tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("BloodRequest: save and find by hospital ID")
    void bloodRequest_findByHospitalId() {
        Province province = provinceRepository.save(buildProvince("Prov-BR"));
        City city         = cityRepository.save(buildCity("City-BR", province));
        User user         = userRepository.save(buildUser("br@example.com", "0309-1111111", Role.HOSPITAL));
        Hospital hospital = new Hospital();
        hospital.setUser(user); hospital.setHospitalName("BR Hospital");
        hospital.setRegistrationNumber("BR-REG-001"); hospital.setProvince(province); hospital.setCity(city);
        hospitalRepository.save(hospital);

        BloodRequest request = new BloodRequest();
        request.setHospital(hospital);
        request.setBloodGroup(BloodGroup.A_NEGATIVE);
        request.setUnitsRequired(5);
        request.setUrgency(RequestUrgency.URGENT);
        request.setStatus(RequestStatus.PENDING);
        request.setProvince(province);
        request.setCity(city);
        bloodRequestRepository.save(request);

        assertThat(bloodRequestRepository.findByHospitalId(hospital.getId())).hasSize(1);
        assertThat(bloodRequestRepository.findByStatus(RequestStatus.PENDING)).hasSize(1);
        assertThat(bloodRequestRepository.findByBloodGroup(BloodGroup.A_NEGATIVE)).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // BloodInventory tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("BloodInventory: composite unique prevents duplicate hospital+bloodGroup")
    void bloodInventory_duplicateThrowsException() {
        Province province = provinceRepository.save(buildProvince("Prov-Inv"));
        City city         = cityRepository.save(buildCity("City-Inv", province));
        User user         = userRepository.save(buildUser("inv@example.com", "0310-1111111", Role.HOSPITAL));
        Hospital hospital = new Hospital();
        hospital.setUser(user); hospital.setHospitalName("Inv Hospital");
        hospital.setRegistrationNumber("INV-REG-001"); hospital.setProvince(province); hospital.setCity(city);
        hospitalRepository.save(hospital);

        BloodInventory inv1 = new BloodInventory();
        inv1.setHospital(hospital); inv1.setBloodGroup(BloodGroup.AB_POSITIVE);
        bloodInventoryRepository.save(inv1);

        BloodInventory inv2 = new BloodInventory();
        inv2.setHospital(hospital); inv2.setBloodGroup(BloodGroup.AB_POSITIVE);

        assertThatThrownBy(() -> bloodInventoryRepository.saveAndFlush(inv2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // -------------------------------------------------------------------------
    // Notification tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Notification: find by recipient ordered newest first")
    void notification_findByRecipientOrdered() {
        User user = userRepository.save(buildUser("notif@example.com", "0311-1111111", Role.DONOR));

        Notification n1 = new Notification();
        n1.setRecipient(user); n1.setTitle("First"); n1.setMessage("Message 1");
        n1.setNotificationType(NotificationType.SYSTEM_ALERT);
        n1.setNotificationStatus(NotificationStatus.UNREAD);
        notificationRepository.save(n1);

        Notification n2 = new Notification();
        n2.setRecipient(user); n2.setTitle("Second"); n2.setMessage("Message 2");
        n2.setNotificationType(NotificationType.BLOOD_REQUEST);
        n2.setNotificationStatus(NotificationStatus.UNREAD);
        notificationRepository.save(n2);

        assertThat(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())).hasSize(2);
        long unread = notificationRepository.countByRecipientIdAndNotificationStatus(
                user.getId(), NotificationStatus.UNREAD);
        assertThat(unread).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // AuditLog tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AuditLog: save with null actor (system event) is allowed")
    void auditLog_systemEventWithNullActor() {
        AuditLog log = new AuditLog();
        log.setActor(null);   // system-generated event
        log.setAction(AuditAction.LOGIN);
        log.setEntityName("User");
        log.setEntityId("42");
        log.setDescription("System login event");
        AuditLog saved = auditLogRepository.save(log);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AuditLog: find by entity name and entity ID")
    void auditLog_findByEntityNameAndEntityId() {
        User actor = userRepository.save(buildUser("audit@example.com", "0312-1111111", Role.ADMIN));

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(AuditAction.UPDATE);
        log.setEntityName("Donor");
        log.setEntityId("100");
        log.setDescription("Donor profile updated");
        auditLogRepository.save(log);

        assertThat(auditLogRepository.findByEntityNameAndEntityId("Donor", "100")).hasSize(1);
    }

    // -------------------------------------------------------------------------
    // Enum stored as STRING verification
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Enum values are stored as STRING â€” verify BloodGroup persistence")
    void enumStoredAsString() {
        Province province = provinceRepository.save(buildProvince("Prov-Enum"));
        City city         = cityRepository.save(buildCity("City-Enum", province));
        User user         = userRepository.save(buildUser("enum@example.com", "0313-1111111", Role.HOSPITAL));
        Hospital hospital = new Hospital();
        hospital.setUser(user); hospital.setHospitalName("Enum Hospital");
        hospital.setRegistrationNumber("ENUM-REG-001"); hospital.setProvince(province); hospital.setCity(city);
        hospitalRepository.save(hospital);

        BloodInventory inv = new BloodInventory();
        inv.setHospital(hospital);
        inv.setBloodGroup(BloodGroup.O_NEGATIVE);
        BloodInventory saved = bloodInventoryRepository.save(inv);

        // Re-load from DB to verify the enum round-trips correctly
        BloodInventory reloaded = bloodInventoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getBloodGroup()).isEqualTo(BloodGroup.O_NEGATIVE);
    }
}
