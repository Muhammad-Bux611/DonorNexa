package com.donornexa.bloodrequest.dto;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestUrgency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Form backing object for hospital blood request creation.
 */
@Getter
@Setter
public class CreateBloodRequestDTO {

    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    @NotNull(message = "Units required is mandatory")
    @Min(value = 1, message = "At least 1 unit of blood must be requested")
    private Integer unitsRequired = 1;

    @NotNull(message = "Urgency level is required")
    private RequestUrgency urgency = RequestUrgency.NORMAL;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime requiredBy;

    @NotNull(message = "Province is required")
    private Long provinceId;

    @NotNull(message = "City is required")
    private Long cityId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String hospitalNotes;
}
