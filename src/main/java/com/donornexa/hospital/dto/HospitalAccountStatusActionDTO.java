package com.donornexa.hospital.dto;

import com.donornexa.common.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for administrator account status transitions (ACTIVE, SUSPENDED, INACTIVE).
 */
@Getter
@Setter
public class HospitalAccountStatusActionDTO {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}
