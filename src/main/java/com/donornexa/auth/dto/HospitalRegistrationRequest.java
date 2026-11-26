package com.donornexa.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Data-transfer object for the hospital registration form.
 * Never bound directly to a JPA entity.
 */
@Getter
@Setter
public class HospitalRegistrationRequest {

    @NotBlank(message = "Hospital name is required")
    @Size(min = 2, max = 200, message = "Hospital name must be 2–200 characters")
    private String hospitalName;

    @NotBlank(message = "Registration number is required")
    @Size(max = 100, message = "Registration number must not exceed 100 characters")
    private String registrationNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 20, message = "Phone number must be 7–20 characters")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @NotNull(message = "Province is required")
    private Long provinceId;

    @NotNull(message = "City is required")
    private Long cityId;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String completeAddress;

    private org.springframework.web.multipart.MultipartFile licenceFile;
}
