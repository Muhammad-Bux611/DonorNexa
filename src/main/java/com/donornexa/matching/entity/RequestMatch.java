package com.donornexa.matching.entity;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.donor.entity.Donor;
import com.donornexa.location.entity.City;


import com.donornexa.common.enums.MatchStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents the matching relationship between a donor and a blood request.
 * A donor can be matched to multiple requests and vice versa.
 * A composite unique constraint prevents the same donor from being
 * matched to the same request more than once.
 */
@Getter
@Setter
@Entity
@Table(
    name = "request_matches",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_request_matches_donor_request",
            columnNames = {"donor_id", "blood_request_id"}
        )
    },
    indexes = {
        @Index(name = "idx_request_matches_donor_id",        columnList = "donor_id"),
        @Index(name = "idx_request_matches_blood_request_id", columnList = "blood_request_id"),
        @Index(name = "idx_request_matches_match_status",    columnList = "match_status")
    }
)
public class RequestMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The blood request this match is associated with.
     * History must be preserved; no cascade removal.
     */
    @NotNull(message = "Blood request is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_request_matches_blood_request"))
    private BloodRequest bloodRequest;

    /**
     * The donor who was matched to this request.
     */
    @NotNull(message = "Donor is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_request_matches_donor"))
    private Donor donor;

    @NotNull(message = "Match status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 15)
    private MatchStatus matchStatus;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "response_at")
    private LocalDateTime responseAt;

    /**
     * Calculated matching score (e.g. 0-100) combining blood compatibility,
     * geographic proximity, donation cooldown buffer, and urgency.
     */
    @Column(name = "match_score")
    private Integer matchScore;

    /**
     * Human-readable explanation of why this donor was matched (e.g. Exact ABO match, Same City).
     */
    @Size(max = 500, message = "Compatibility summary must not exceed 500 characters")
    @Column(name = "compatibility_summary", length = 500)
    private String compatibilitySummary;

    /**
     * Whether an in-app emergency notification has been dispatched to the donor.
     */
    @Column(name = "notified", nullable = false)
    private Boolean notified = Boolean.FALSE;

    /**
     * Optional response notes provided by the donor upon accepting or declining.
     */
    @Size(max = 500, message = "Donor notes must not exceed 500 characters")
    @Column(name = "donor_notes", length = 500)
    private String donorNotes;

    @Column(name = "scheduled_appointment_time")
    private LocalDateTime scheduledAppointmentTime;

    @Size(max = 500, message = "Appointment notes must not exceed 500 characters")
    @Column(name = "appointment_notes", length = 500)
    private String appointmentNotes;

    @Column(name = "units_donated")
    private Integer unitsDonated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id", foreignKey = @ForeignKey(name = "fk_request_matches_donation"))
    private Donation donation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.matchStatus == null) {
            this.matchStatus = MatchStatus.PENDING;
        }
        if (this.matchedAt == null) {
            this.matchedAt = now;
        }
        if (this.notified == null) {
            this.notified = Boolean.FALSE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "RequestMatch{id=" + id + ", matchStatus=" + matchStatus + "}";
    }
}
