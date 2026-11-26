package com.donornexa.hospital.dto;

import com.donornexa.common.enums.AccountStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Read-only representation of a hospital's profile and administrative status.
 */
@Getter
@Builder
public class HospitalProfileDTO {

    private Long id;
    private Long userId;
    private String hospitalName;
    private String registrationNumber;
    private String email;
    private String contactNumber;
    private Long provinceId;
    private String provinceName;
    private Long cityId;
    private String cityName;
    private String completeAddress;
    private String licenceFile;
    private Boolean verified;
    private String rejectionReason;
    private HospitalVerificationStatus verificationStatus;
    private AccountStatus accountStatus;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
