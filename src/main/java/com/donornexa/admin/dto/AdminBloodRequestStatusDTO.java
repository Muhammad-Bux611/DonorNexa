package com.donornexa.admin.dto;

import com.donornexa.common.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for administrator-managed blood request status transitions.
 */
@Getter
@Setter
public class AdminBloodRequestStatusDTO {

    @NotNull(message = "Request status is required")
    private RequestStatus status;
}
