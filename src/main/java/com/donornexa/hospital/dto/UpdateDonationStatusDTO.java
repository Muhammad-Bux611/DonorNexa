package com.donornexa.hospital.dto;

import com.donornexa.common.enums.DonationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDonationStatusDTO {

    @NotNull(message = "Target status is required")
    private DonationStatus status;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private boolean addToInventory = true;
}
