package com.donornexa.hospital.dto;

import com.donornexa.common.enums.BloodGroup;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueStockDTO {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Units to issue is required")
    @Min(value = 1, message = "Minimum 1 unit required")
    @Max(value = 50, message = "Maximum 50 units may be issued per transaction")
    private Integer units = 1;

    private Long bloodRequestId;

    @Size(max = 1000, message = "Clinical dispatch notes must not exceed 1000 characters")
    private String notes;
}
