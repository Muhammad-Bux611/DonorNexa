package com.donornexa.auth.service;

import com.donornexa.auth.dto.DonorRegistrationRequest;
import com.donornexa.auth.dto.HospitalRegistrationRequest;
import com.donornexa.common.exception.RegistrationException;
import com.donornexa.common.enums.Role;
import com.donornexa.location.entity.City;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;
import com.donornexa.common.enums.AccountStatus;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.location.repository.CityRepository;
import com.donornexa.donor.repository.DonorRepository;
import com.donornexa.hospital.repository.HospitalRepository;
import com.donornexa.location.repository.ProvinceRepository;
import com.donornexa.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication and registration service.
 *
 * <p>All registration operations are wrapped in a single transaction so that
 * the User and the associated profile (Donor or Hospital) are saved atomically.
 * If any step fails the entire transaction is rolled back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final DonorRepository      donorRepository;
    private final HospitalRepository   hospitalRepository;
    private final ProvinceRepository   provinceRepository;
    private final CityRepository       cityRepository;
    private final PasswordEncoder      passwordEncoder;
    private final com.donornexa.common.service.FileUploadService fileUploadService;

    // -------------------------------------------------------------------------
    // Donor registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new donor.
     *
     * @throws RegistrationException if any uniqueness or business rule is violated.
     */
    @Transactional
    public void registerDonor(DonorRegistrationRequest request) {
        validateCommonFields(request.getEmail(), request.getPhoneNumber(),
                             request.getPassword(), request.getConfirmPassword());

        Province province = resolveProvince(request.getProvinceId());
        City     city     = resolveCity(request.getCityId(), request.getProvinceId());

        // Build and persist the User
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.DONOR);
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEnabled(Boolean.TRUE);
        userRepository.save(user);

        // Build and persist the Donor profile
        Donor donor = new Donor();
        donor.setUser(user);
        donor.setBloodGroup(request.getBloodGroup());
        donor.setDateOfBirth(request.getDateOfBirth());
        donor.setGender(request.getGender());
        donor.setProvince(province);
        donor.setCity(city);
        donor.setCompleteAddress(request.getCompleteAddress());
        donor.setAvailability(DonorAvailability.AVAILABLE);
        donor.setVerified(Boolean.FALSE);

        if (request.getPhotoFile() != null && !request.getPhotoFile().isEmpty() && fileUploadService != null) {
            String photoName = fileUploadService.uploadDonorPhoto(request.getPhotoFile());
            donor.setPhoto(photoName);
        }

        donorRepository.save(donor);

        log.info("Donor registered: {}", user.getEmail());
    }

    // -------------------------------------------------------------------------
    // Hospital registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new hospital.
     *
     * @throws RegistrationException if any uniqueness or business rule is violated.
     */
    @Transactional
    public void registerHospital(HospitalRegistrationRequest request) {
        validateCommonFields(request.getEmail(), request.getPhoneNumber(),
                             request.getPassword(), request.getConfirmPassword());

        if (hospitalRepository.existsByRegistrationNumber(request.getRegistrationNumber().trim())) {
            throw new RegistrationException(
                    "A hospital with registration number '"
                    + request.getRegistrationNumber() + "' is already registered.");
        }

        Province province = resolveProvince(request.getProvinceId());
        City     city     = resolveCity(request.getCityId(), request.getProvinceId());

        // Build and persist the User
        User user = new User();
        user.setFullName(request.getHospitalName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.HOSPITAL);
        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setEnabled(Boolean.TRUE);
        userRepository.save(user);

        // Build and persist the Hospital profile
        Hospital hospital = new Hospital();
        hospital.setUser(user);
        hospital.setHospitalName(request.getHospitalName());
        hospital.setRegistrationNumber(request.getRegistrationNumber().trim());
        hospital.setProvince(province);
        hospital.setCity(city);
        hospital.setCompleteAddress(request.getCompleteAddress());
        hospital.setContactNumber(request.getPhoneNumber().trim());
        hospital.setVerified(Boolean.FALSE);
        hospital.setTrustScore(100);

        if (request.getLicenceFile() != null && !request.getLicenceFile().isEmpty() && fileUploadService != null) {
            String licenceName = fileUploadService.uploadHospitalLicence(request.getLicenceFile());
            hospital.setLicenceFile(licenceName);
        }

        hospitalRepository.save(hospital);

        log.info("Hospital registered: {}", user.getEmail());
    }

    // -------------------------------------------------------------------------
    // Shared validation helpers
    // -------------------------------------------------------------------------

    private void validateCommonFields(String email, String phone,
                                      String password, String confirmPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RegistrationException(
                    "An account with email '" + email + "' already exists.");
        }
        if (userRepository.existsByPhoneNumber(phone.trim())) {
            throw new RegistrationException(
                    "An account with phone number '" + phone + "' already exists.");
        }
        if (!password.equals(confirmPassword)) {
            throw new RegistrationException(
                    "Passwords do not match. Please re-enter your password.");
        }
    }

    private Province resolveProvince(Long provinceId) {
        return provinceRepository.findById(provinceId)
                .orElseThrow(() -> new RegistrationException(
                        "Selected province is invalid. Please choose a valid province."));
    }

    private City resolveCity(Long cityId, Long provinceId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new RegistrationException(
                        "Selected city is invalid. Please choose a valid city."));
        if (!city.getProvince().getId().equals(provinceId)) {
            throw new RegistrationException(
                    "Selected city does not belong to the selected province.");
        }
        return city;
    }
}
