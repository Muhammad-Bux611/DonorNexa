package com.donornexa.inventory.entity;

import com.donornexa.common.enums.InventoryStatus;
import com.donornexa.hospital.entity.Hospital;


import com.donornexa.common.enums.BloodGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maintains the blood inventory for a hospital, broken down by blood group.
 * A composite unique constraint ensures each hospital has at most one inventory
 * record per blood group.
 */
@Getter
@Setter
@Entity
@Table(
    name = "blood_inventory",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_blood_inventory_hospital_blood_group",
            columnNames = {"hospital_id", "blood_group"}
        )
    },
    indexes = {
        @Index(name = "idx_blood_inventory_hospital_id", columnList = "hospital_id"),
        @Index(name = "idx_blood_inventory_blood_group", columnList = "blood_group")
    }
)
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_blood_inventory_hospital"))
    private Hospital hospital;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @Min(value = 0, message = "Available units cannot be negative")
    @Column(name = "available_units", nullable = false)
    private Integer availableUnits = 0;

    @Min(value = 0, message = "Reserved units cannot be negative")
    @Column(name = "reserved_units", nullable = false)
    private Integer reservedUnits = 0;

    @Column(name = "collection_date")
    private LocalDateTime collectionDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", length = 20)
    private com.donornexa.common.enums.InventoryStatus inventoryStatus = com.donornexa.common.enums.InventoryStatus.OUT_OF_STOCK;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void recalculateStatus() {
        if (this.availableUnits == null || this.availableUnits <= 0) {
            this.inventoryStatus = com.donornexa.common.enums.InventoryStatus.OUT_OF_STOCK;
        } else if (this.availableUnits < 3) {
            this.inventoryStatus = com.donornexa.common.enums.InventoryStatus.LOW_STOCK;
        } else {
            this.inventoryStatus = com.donornexa.common.enums.InventoryStatus.AVAILABLE;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUpdated = now;
        if (this.availableUnits == null) {
            this.availableUnits = 0;
        }
        if (this.reservedUnits == null) {
            this.reservedUnits = 0;
        }
        recalculateStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        this.updatedAt = now;
        this.lastUpdated = now;
        recalculateStatus();
    }

    @Override
    public String toString() {
        return "BloodInventory{id=" + id + ", bloodGroup=" + bloodGroup
                + ", availableUnits=" + availableUnits + ", reservedUnits=" + reservedUnits + "}";
    }
}
