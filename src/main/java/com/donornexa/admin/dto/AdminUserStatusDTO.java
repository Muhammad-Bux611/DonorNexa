package com.donornexa.admin.dto;

import com.donornexa.common.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for administrator-managed user account status updates.
 */
@Getter
@Setter
public class AdminUserStatusDTO {

    @NotNull(message = "Account status is required")
    private AccountStatus accountStatus;
}
