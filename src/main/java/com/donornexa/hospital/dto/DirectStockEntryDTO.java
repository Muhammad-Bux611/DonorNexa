package com.donornexa.hospital.dto;

import com.donornexa.common.enums.BloodGroup;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class DirectStockEntryDTO {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Units count is required")
    @Min(value = 1, message = "Minimum 1 unit required")
    @Max(value = 100, message = "Maximum 100 units can be added in a single batch")
    private Integer units = 1;

    @NotNull(message = "Collection date is required")
    @PastOrPresent(message = "Collection date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate collectionDate = LocalDate.now();

    @NotNull(message = "Shelf life duration is required")
    @Min(value = 1, message = "Shelf life must be at least 1 day")
    @Max(value = 42, message = "Whole blood shelf life cannot exceed 42 days under standard SAGM/CPDA-1 protocol")
    private Integer shelfLifeDays = 35;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
