package com.donornexa.donor.dto;

import com.donornexa.common.enums.DonorAvailability;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for updating donor donation availability.
 */
@Getter
@Setter
public class DonorAvailabilityUpdateRequest {

    @NotNull(message = "Availability status is required")
    private DonorAvailability availability;
}
