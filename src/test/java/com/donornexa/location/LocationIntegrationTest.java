package com.donornexa.location;

import com.donornexa.auth.dto.DonorRegistrationRequest;
import com.donornexa.auth.dto.HospitalRegistrationRequest;
import com.donornexa.common.exception.RegistrationException;
import com.donornexa.auth.service.AuthService;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.Gender;
import com.donornexa.location.service.LocationDataSeeder;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test suite for dynamic cascading Province and City dropdowns,
 * database seeding idempotency, and location validation during registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Location & Cascading Dropdown Tests")
class LocationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private LocationDataSeeder locationDataSeeder;
    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private DonorRepository donorRepository;
    @Autowired private HospitalRepository hospitalRepository;

    // =========================================================================
    // 1. Province dropdown loads successfully via GET /api/locations/provinces
    // =========================================================================

    @Test
    @DisplayName("1. Province dropdown loads successfully via GET /api/locations/provinces")
    void provincesEndpoint_loadsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/locations/provinces")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(not(empty()))));
    }

    // =========================================================================
    // 2. All seven administrative areas appear
    // =========================================================================

    @Test
    @DisplayName("2. All seven Pakistan administrative areas appear")
    void provincesEndpoint_allSevenAdministrativeAreasAppear() throws Exception {
        mockMvc.perform(get("/api/locations/provinces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "Punjab",
                        "Sindh",
                        "Khyber Pakhtunkhwa",
                        "Balochistan",
                        "Islamabad Capital Territory",
                        "Gilgit-Baltistan",
                        "Azad Jammu and Kashmir"
                )));
    }

    // =========================================================================
    // 3. Selecting Sindh loads Sindh cities
    // =========================================================================

    @Test
    @DisplayName("3. Selecting Sindh loads Sindh cities")
    void citiesEndpoint_sindhCitiesLoad() throws Exception {
        Province sindh = provinceRepository.findByName("Sindh").orElseThrow();

        mockMvc.perform(get("/api/locations/provinces/{id}/cities", sindh.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "Karachi", "Hyderabad", "Sukkur", "Larkana", "Mirpur Khas"
                )));
    }

    // =========================================================================
    // 4. Selecting Punjab loads Punjab cities
    // =========================================================================

    @Test
    @DisplayName("4. Selecting Punjab loads Punjab cities")
    void citiesEndpoint_punjabCitiesLoad() throws Exception {
        Province punjab = provinceRepository.findByName("Punjab").orElseThrow();

        mockMvc.perform(get("/api/locations/provinces/{id}/cities", punjab.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "Lahore", "Faisalabad", "Rawalpindi", "Multan", "Gujranwala"
                )));
    }

    // =========================================================================
    // 5. Selecting Khyber Pakhtunkhwa loads its cities
    // =========================================================================

    @Test
    @DisplayName("5. Selecting Khyber Pakhtunkhwa loads KPK cities")
    void citiesEndpoint_kpkCitiesLoad() throws Exception {
        Province kpk = provinceRepository.findByName("Khyber Pakhtunkhwa").orElseThrow();

        mockMvc.perform(get("/api/locations/provinces/{id}/cities", kpk.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "Peshawar", "Mardan", "Abbottabad", "Swat", "Kohat"
                )));
    }

    // =========================================================================
    // 6. Selecting Balochistan loads its cities
    // =========================================================================

    @Test
    @DisplayName("6. Selecting Balochistan loads Balochistan cities")
    void citiesEndpoint_balochistanCitiesLoad() throws Exception {
        Province balochistan = provinceRepository.findByName("Balochistan").orElseThrow();

        mockMvc.perform(get("/api/locations/provinces/{id}/cities", balochistan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[*].name", hasItems(
                        "Quetta", "Gwadar", "Turbat", "Khuzdar", "Chaman"
                )));
    }

    // =========================================================================
    // 7. City dropdown placeholder and initial state on registration pages
    // =========================================================================

    @Test
    @DisplayName("7. Donor registration page renders location elements and placeholder")
    void donorRegisterPage_containsLocationDropdowns() throws Exception {
        mockMvc.perform(get("/register/donor"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"provinceSelect\"")))
                .andExpect(content().string(containsString("id=\"citySelect\"")))
                .andExpect(content().string(containsString("Select Province")))
                .andExpect(content().string(containsString("Select Province First")))
                .andExpect(content().string(containsString("location-dropdown.js")));
    }

    @Test
    @DisplayName("8. Hospital registration page renders location elements and placeholder")
    void hospitalRegisterPage_containsLocationDropdowns() throws Exception {
        mockMvc.perform(get("/register/hospital"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"provinceSelect\"")))
                .andExpect(content().string(containsString("id=\"citySelect\"")))
                .andExpect(content().string(containsString("Select Province")))
                .andExpect(content().string(containsString("Select Province First")))
                .andExpect(content().string(containsString("location-dropdown.js")));
    }

    // =========================================================================
    // 9. Invalid province IDs are handled correctly (404 Not Found)
    // =========================================================================

    @Test
    @DisplayName("9. Invalid province ID returns 404 Not Found")
    void citiesEndpoint_invalidProvinceId_returns404() throws Exception {
        mockMvc.perform(get("/api/locations/provinces/{id}/cities", 999999L))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 10. Invalid city/province combinations are rejected during registration
    // =========================================================================

    @Test
    @DisplayName("10. Invalid city/province combination is rejected with RegistrationException")
    void registration_mismatchedCityAndProvince_rejected() {
        Province punjab = provinceRepository.findByName("Punjab").orElseThrow();
        Province sindh = provinceRepository.findByName("Sindh").orElseThrow();
        City karachi = cityRepository.findByName("Karachi").stream()
                .filter(c -> c.getProvince().getId().equals(sindh.getId()))
                .findFirst()
                .orElseThrow();

        DonorRegistrationRequest request = new DonorRegistrationRequest();
        request.setFullName("Test Mismatch");
        request.setEmail("mismatch@example.com");
        request.setPhoneNumber("0300-8888888");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        request.setBloodGroup(BloodGroup.O_POSITIVE);
        request.setGender(Gender.MALE);
        request.setDateOfBirth(LocalDate.of(1995, 1, 1));
        // Intentional manipulation: Province Punjab with City Karachi (Sindh)
        request.setProvinceId(punjab.getId());
        request.setCityId(karachi.getId());
        request.setCompleteAddress("Test Address");

        assertThatThrownBy(() -> authService.registerDonor(request))
                .isInstanceOf(RegistrationException.class)
                .hasMessageContaining("Selected city does not belong to the selected province");
    }

    // =========================================================================
    // 11. Donor registration saves the correct province and city
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("11. Donor registration saves the correct province and city")
    void donorRegistration_savesCorrectProvinceAndCity() {
        Province punjab = provinceRepository.findByName("Punjab").orElseThrow();
        City lahore = cityRepository.findByName("Lahore").stream()
                .filter(c -> c.getProvince().getId().equals(punjab.getId()))
                .findFirst()
                .orElseThrow();

        DonorRegistrationRequest request = new DonorRegistrationRequest();
        request.setFullName("Punjab Donor");
        request.setEmail("punjab.donor@example.com");
        request.setPhoneNumber("0321-7654321");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        request.setBloodGroup(BloodGroup.B_POSITIVE);
        request.setGender(Gender.FEMALE);
        request.setDateOfBirth(LocalDate.of(1998, 7, 20));
        request.setProvinceId(punjab.getId());
        request.setCityId(lahore.getId());
        request.setCompleteAddress("Gulberg, Lahore");

        authService.registerDonor(request);

        User user = userRepository.findByEmail("punjab.donor@example.com").orElseThrow();
        Donor donor = donorRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(donor.getProvince().getId()).isEqualTo(punjab.getId());
        assertThat(donor.getProvince().getName()).isEqualTo("Punjab");
        assertThat(donor.getCity().getId()).isEqualTo(lahore.getId());
        assertThat(donor.getCity().getName()).isEqualTo("Lahore");
    }

    // =========================================================================
    // 12. Hospital registration saves the correct province and city
    // =========================================================================

    @Test
    @Transactional
    @DisplayName("12. Hospital registration saves the correct province and city")
    void hospitalRegistration_savesCorrectProvinceAndCity() {
        Province sindh = provinceRepository.findByName("Sindh").orElseThrow();
        City karachi = cityRepository.findByName("Karachi").stream()
                .filter(c -> c.getProvince().getId().equals(sindh.getId()))
                .findFirst()
                .orElseThrow();

        HospitalRegistrationRequest request = new HospitalRegistrationRequest();
        request.setHospitalName("Karachi Care Hospital");
        request.setRegistrationNumber("HOSP-KHI-001");
        request.setEmail("info@karachicare.org");
        request.setPhoneNumber("021-34567890");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        request.setProvinceId(sindh.getId());
        request.setCityId(karachi.getId());
        request.setCompleteAddress("Clifton, Karachi");

        authService.registerHospital(request);

        User user = userRepository.findByEmail("info@karachicare.org").orElseThrow();
        Hospital hospital = hospitalRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(hospital.getProvince().getId()).isEqualTo(sindh.getId());
        assertThat(hospital.getProvince().getName()).isEqualTo("Sindh");
        assertThat(hospital.getCity().getId()).isEqualTo(karachi.getId());
        assertThat(hospital.getCity().getName()).isEqualTo("Karachi");
    }

    // =========================================================================
    // 13. Database seeding is idempotent â€” no duplicates on repeated runs
    // =========================================================================

    @Test
    @DisplayName("13. Database seeding does not create duplicate records on multiple runs")
    void databaseSeeding_idempotent_noDuplicates() {
        long initialProvinceCount = provinceRepository.count();
        long initialCityCount = cityRepository.count();

        assertThat(initialProvinceCount).isEqualTo(7);
        assertThat(initialCityCount).isEqualTo(50);

        // Run seeder again simulating application restart
        locationDataSeeder.seedLocations();

        long postSeedingProvinceCount = provinceRepository.count();
        long postSeedingCityCount = cityRepository.count();

        assertThat(postSeedingProvinceCount).isEqualTo(initialProvinceCount);
        assertThat(postSeedingCityCount).isEqualTo(initialCityCount);
    }
}
