package com.donornexa.donor.entity;

import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;


import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.DonorAvailability;
import com.donornexa.common.enums.Gender;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a blood donor's profile.
 * Each donor profile is linked to exactly one user account.
 * Medical eligibility logic is not enforced at the entity level.
 */
@Getter
@Setter
@Entity
@Table(
    name = "donors",
    indexes = {
        @Index(name = "idx_donors_user_id",      columnList = "user_id"),
        @Index(name = "idx_donors_blood_group",  columnList = "blood_group"),
        @Index(name = "idx_donors_availability", columnList = "availability"),
        @Index(name = "idx_donors_province_id",  columnList = "province_id"),
        @Index(name = "idx_donors_city_id",      columnList = "city_id")
    }
)
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One-to-One relationship with User.
     * A user can only have one donor profile.
     */
    @NotNull(message = "User account is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_donors_user"))
    private User user;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    /**
     * Donor weight in kilograms. Must be positive when provided.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be positive")
    @Digits(integer = 3, fraction = 2, message = "Weight must be a valid number (e.g. 65.50)")
    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @NotNull(message = "Province is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_donors_province"))
    private Province province;

    @NotNull(message = "City is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_donors_city"))
    private City city;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "complete_address", length = 500)
    private String completeAddress;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Size(max = 255, message = "Photo filename must not exceed 255 characters")
    @Column(name = "photo", length = 255)
    private String photo;

    @NotNull(message = "Availability status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "availability", nullable = false, length = 30)
    private DonorAvailability availability;

    @Column(name = "verified", nullable = false)
    private Boolean verified = Boolean.FALSE;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Compute verification status:
     * - VERIFIED if verified flag is true
     * - REJECTED if verified flag is false and a rejection reason exists
     * - PENDING otherwise
     */
    @Transient
    public com.donornexa.donor.dto.DonorVerificationStatus getVerificationStatus() {
        if (Boolean.TRUE.equals(this.verified)) {
            return com.donornexa.donor.dto.DonorVerificationStatus.VERIFIED;
        }
        if (this.rejectionReason != null && !this.rejectionReason.isBlank()) {
            return com.donornexa.donor.dto.DonorVerificationStatus.REJECTED;
        }
        return com.donornexa.donor.dto.DonorVerificationStatus.PENDING;
    }

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.availability == null) {
            this.availability = DonorAvailability.AVAILABLE;
        }
        if (this.verified == null) {
            this.verified = Boolean.FALSE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Donor{id=" + id + ", bloodGroup=" + bloodGroup + ", availability=" + availability + "}";
    }
}
