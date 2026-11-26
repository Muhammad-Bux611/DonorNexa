package com.donornexa.hospital.entity;

import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;
import com.donornexa.user.entity.User;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a hospital registered in the system.
 * Each hospital is linked to exactly one user account.
 * Hospitals can post blood requests and receive donated blood.
 */
@Getter
@Setter
@Entity
@Table(
    name = "hospitals",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_hospitals_registration_number", columnNames = "registration_number")
    },
    indexes = {
        @Index(name = "idx_hospitals_user_id",             columnList = "user_id"),
        @Index(name = "idx_hospitals_province_id",         columnList = "province_id"),
        @Index(name = "idx_hospitals_city_id",             columnList = "city_id"),
        @Index(name = "idx_hospitals_registration_number", columnList = "registration_number")
    }
)
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * One-to-One relationship with User.
     * A user can only have one hospital profile.
     */
    @NotNull(message = "User account is required")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_hospitals_user"))
    private User user;

    @NotBlank(message = "Hospital name is required")
    @Size(max = 200, message = "Hospital name must not exceed 200 characters")
    @Column(name = "hospital_name", nullable = false, length = 200)
    private String hospitalName;

    @NotBlank(message = "Registration number is required")
    @Size(max = 100, message = "Registration number must not exceed 100 characters")
    @Column(name = "registration_number", nullable = false, unique = true, length = 100)
    private String registrationNumber;

    @NotNull(message = "Province is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_hospitals_province"))
    private Province province;

    @NotNull(message = "City is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_hospitals_city"))
    private City city;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    @Column(name = "complete_address", length = 500)
    private String completeAddress;

    @Size(max = 20, message = "Contact number must not exceed 20 characters")
    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Size(max = 255, message = "Licence file path must not exceed 255 characters")
    @Column(name = "licence_file", length = 255)
    private String licenceFile;

    @Column(name = "trust_score", nullable = false)
    private Integer trustScore = 100;

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
    public com.donornexa.hospital.dto.HospitalVerificationStatus getVerificationStatus() {
        if (Boolean.TRUE.equals(this.verified)) {
            return com.donornexa.hospital.dto.HospitalVerificationStatus.VERIFIED;
        }
        if (this.rejectionReason != null && !this.rejectionReason.isBlank()) {
            return com.donornexa.hospital.dto.HospitalVerificationStatus.REJECTED;
        }
        return com.donornexa.hospital.dto.HospitalVerificationStatus.PENDING;
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
        if (this.verified == null) {
            this.verified = Boolean.FALSE;
        }
        if (this.trustScore == null) {
            this.trustScore = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Hospital{id=" + id + ", hospitalName='" + hospitalName + "', registrationNumber='" + registrationNumber + "'}";
    }
}
