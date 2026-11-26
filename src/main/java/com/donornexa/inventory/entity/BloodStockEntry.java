package com.donornexa.inventory.entity;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donation.entity.Donation;
import com.donornexa.hospital.entity.Hospital;


import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.StockEntryType;
import com.donornexa.common.enums.StockItemStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a traceable batch of blood units collected or issued by a hospital.
 * Tracks collection date, expiry date, associated donation or blood request,
 * and current lifecycle status (AVAILABLE, ISSUED, EXPIRED, DISCARDED).
 */
@Getter
@Setter
@Entity
@Table(
    name = "blood_stock_entries",
    indexes = {
        @Index(name = "idx_stock_hospital_id", columnList = "hospital_id"),
        @Index(name = "idx_stock_blood_group", columnList = "blood_group"),
        @Index(name = "idx_stock_status", columnList = "status"),
        @Index(name = "idx_stock_expiry_date", columnList = "expiry_date"),
        @Index(name = "idx_stock_donation_id", columnList = "donation_id"),
        @Index(name = "idx_stock_request_id", columnList = "blood_request_id")
    }
)
public class BloodStockEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_stock_hospital"))
    private Hospital hospital;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @NotNull(message = "Units count is required")
    @Min(value = 1, message = "Units count must be at least 1")
    @Column(name = "units", nullable = false)
    private Integer units;

    @NotNull(message = "Collection date is required")
    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @NotNull(message = "Stock entry type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 25)
    private StockEntryType entryType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockItemStatus status = StockItemStatus.AVAILABLE;

    /**
     * Optional link to the voluntary/request-based donation from which this unit originated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donation_id",
                foreignKey = @ForeignKey(name = "fk_stock_donation"))
    private Donation donation;

    /**
     * Optional link to the blood request against which this stock entry was issued or reserved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_request_id",
                foreignKey = @ForeignKey(name = "fk_stock_blood_request"))
    private BloodRequest bloodRequest;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isNearExpiry(int thresholdDays) {
        if (expiryDate == null || isExpired()) return false;
        LocalDate threshold = LocalDate.now().plusDays(thresholdDays);
        return !expiryDate.isAfter(threshold);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = StockItemStatus.AVAILABLE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
