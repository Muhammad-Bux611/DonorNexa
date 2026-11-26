package com.donornexa.hospital.dto;

import com.donornexa.common.enums.DonationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecordDonationDTO {

    @NotNull(message = "Please select a registered voluntary donor")
    private Long donorId;

    private Long bloodRequestId;

    @NotNull(message = "Units donated is required")
    @Min(value = 1, message = "Minimum 1 blood unit required")
    @Max(value = 5, message = "Maximum 5 units may be recorded in a single donation session")
    private Integer unitsDonated = 1;

    @NotNull(message = "Donation date and time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime donationDate = LocalDateTime.now();

    @NotNull(message = "Donation status is required")
    private DonationStatus donationStatus = DonationStatus.COMPLETED;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    private boolean addToInventory = true;
}
