package com.donornexa.matching.dto;

import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.MatchStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO representing a matched donor recommendation for a blood request.
 */
@Getter
@Builder
public class DonorMatchDTO {

    private Long matchId;
    private Long bloodRequestId;
    private Long donorId;
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private String maskedDonorPhone;
    private BloodGroup donorBloodGroup;
    private String donorProvince;
    private String donorCity;
    private DonorAvailability availability;
    private boolean verified;
    private Integer matchScore;
    private String compatibilitySummary;
    private MatchStatus matchStatus;
    private boolean notified;
    private LocalDateTime matchedAt;
    private LocalDateTime responseAt;
    private String donorNotes;
    private LocalDateTime scheduledAppointmentTime;
    private String appointmentNotes;
    private Integer unitsDonated;

    // Geographic closeness indicator
    private boolean sameCity;
    private boolean sameProvince;

    // Blood match exactness
    private boolean exactBloodMatch;
}
