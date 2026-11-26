package com.donornexa.donation.entity;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;


import com.donornexa.common.enums.DonationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records a blood donation event.
 * Each donation belongs to a donor and a hospital.
 * Association with a blood request is optional â€” a donor may donate voluntarily
 * without a specific active request.
 */
@Getter
@Setter
@Entity
@Table(
    name = "donations",
    indexes = {
        @Index(name = "idx_donations_donor_id",        columnList = "donor_id"),
        @Index(name = "idx_donations_hospital_id",     columnList = "hospital_id"),
        @Index(name = "idx_donations_blood_request_id", columnList = "blood_request_id"),
        @Index(name = "idx_donations_donation_status", columnList = "donation_status"),
        @Index(name = "idx_donations_donation_date",   columnList = "donation_date")
    }
)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Donor is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_donations_donor"))
    private Donor donor;

    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_donations_hospital"))
    private Hospital hospital;

    /**
     * Optional link to the blood request this donation fulfils.
     * Null for voluntary/walk-in donations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_request_id",
                foreignKey = @ForeignKey(name = "fk_donations_blood_request"))
    private BloodRequest bloodRequest;

    @NotNull(message = "Units donated is required")
    @Min(value = 1, message = "Units donated must be at least 1")
    @Column(name = "units_donated", nullable = false)
    private Integer unitsDonated;

    @NotNull(message = "Donation date is required")
    @Column(name = "donation_date", nullable = false)
    private LocalDateTime donationDate;

    @NotNull(message = "Donation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "donation_status", nullable = false, length = 15)
    private DonationStatus donationStatus;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "inventory_added", nullable = false)
    private Boolean inventoryAdded = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.donationStatus == null) {
            this.donationStatus = DonationStatus.SCHEDULED;
        }
        if (this.inventoryAdded == null) {
            this.inventoryAdded = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Donation{id=" + id + ", unitsDonated=" + unitsDonated + ", donationStatus=" + donationStatus + "}";
    }
}
