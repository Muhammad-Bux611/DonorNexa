package com.donornexa.auth.dto;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Data-transfer object for the donor registration form.
 * Never bound directly to a JPA entity.
 */
@Getter
@Setter
public class DonorRegistrationRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be 2â€“100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(03[0-9]{2}-?[0-9]{7}|\\+92-?3[0-9]{2}-?[0-9]{7})$",
             message = "Phone number must be a valid Pakistani mobile number (03XX-XXXXXXX)")
    private String phoneNumber;

    @Pattern(regexp = "^$|^[0-9]{5}-[0-9]{7}-[0-9]$", message = "CNIC must follow the format #####-#######-#")
    private String cnic;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotNull(message = "Province is required")
    private Long provinceId;

    @NotNull(message = "City is required")
    private Long cityId;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String completeAddress;

    private org.springframework.web.multipart.MultipartFile photoFile;
}
