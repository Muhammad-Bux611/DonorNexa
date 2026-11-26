package com.donornexa.donor.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO carrying donor profile update requests.
 * Only permitted fields may be edited; immutable records such as
 * role, verification status, email, blood group, and date of birth
 * are not exposed or modifiable through this DTO.
 */
@Getter
@Setter
public class DonorProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
    private String phoneNumber;

    /**
     * Optional weight in kg. Must be realistic if provided.
     */
    @DecimalMin(value = "30.0", message = "Weight must be at least 30.0 kg")
    @DecimalMax(value = "300.0", message = "Weight must not exceed 300.0 kg")
    @Digits(integer = 3, fraction = 2, message = "Weight must be a valid number (e.g. 65.50)")
    private BigDecimal weight;

    @NotNull(message = "Province is required")
    private Long provinceId;

    @NotNull(message = "City is required")
    private Long cityId;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String completeAddress;

    /**
     * Optional profile photo file (JPEG or PNG, max 2MB).
     */
    private org.springframework.web.multipart.MultipartFile photoFile;
}
