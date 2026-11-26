package com.donornexa.hospital.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Form DTO for updating permitted hospital profile fields.
 * Strictly isolates mutable fields from immutable database attributes
 * (such as registration number, verification, account status, and role).
 */
@Getter
@Setter
public class HospitalProfileUpdateDTO {

    @NotBlank(message = "Hospital name is required")
    @Size(max = 200, message = "Hospital name must not exceed 200 characters")
    private String hospitalName;

    @NotBlank(message = "Contact phone number is required")
    @Size(max = 20, message = "Contact phone number must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$",
             message = "Phone number must contain between 7 and 20 valid characters")
    private String contactNumber;

    @NotNull(message = "Province is required")
    private Long provinceId;

    @NotNull(message = "City is required")
    private Long cityId;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String completeAddress;
}
