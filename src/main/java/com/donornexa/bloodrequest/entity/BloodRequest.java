package com.donornexa.bloodrequest.entity;

import com.donornexa.hospital.entity.Hospital;
import com.donornexa.location.entity.City;
import com.donornexa.location.entity.Province;


import com.donornexa.common.enums.BloodGroup;
import com.donornexa.common.enums.RequestStatus;
import com.donornexa.common.enums.RequestUrgency;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a hospital's request for a specific blood group.
 * Blood requests go through a lifecycle: PENDING â†’ MATCHING â†’
 * PARTIALLY_FULFILLED â†’ FULFILLED, or they can be CANCELLED / EXPIRED.
 */
@Getter
@Setter
@Entity
@Table(
    name = "blood_requests",
    indexes = {
        @Index(name = "idx_blood_requests_hospital_id",  columnList = "hospital_id"),
        @Index(name = "idx_blood_requests_blood_group",  columnList = "blood_group"),
        @Index(name = "idx_blood_requests_status",       columnList = "status"),
        @Index(name = "idx_blood_requests_urgency",      columnList = "urgency"),
        @Index(name = "idx_blood_requests_province_id",  columnList = "province_id"),
        @Index(name = "idx_blood_requests_city_id",      columnList = "city_id"),
        @Index(name = "idx_blood_requests_required_by",  columnList = "required_by")
    }
)
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The hospital that raised this blood request.
     * CascadeType.REMOVE is intentionally omitted to preserve request history.
     */
    @NotNull(message = "Hospital is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_blood_requests_hospital"))
    private Hospital hospital;

    @NotNull(message = "Blood group is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 15)
    private BloodGroup bloodGroup;

    @NotNull(message = "Units required is required")
    @Min(value = 1, message = "Units required must be at least 1")
    @Column(name = "units_required", nullable = false)
    private Integer unitsRequired;

    /**
     * Units fulfilled cannot be negative and cannot exceed unitsRequired.
     * This is enforced in the service layer; the constraint here prevents obvious corruption.
     */
    @Min(value = 0, message = "Units fulfilled cannot be negative")
    @Column(name = "units_fulfilled", nullable = false)
    private Integer unitsFulfilled = 0;

    @NotNull(message = "Urgency level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 15)
    private RequestUrgency urgency;

    @NotNull(message = "Request status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private RequestStatus status;

    @Column(name = "required_by")
    private LocalDateTime requiredBy;

    @NotNull(message = "Province is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_blood_requests_province"))
    private Province province;

    @NotNull(message = "City is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_blood_requests_city"))
    private City city;

    @Size(max = 1000, message = "Hospital notes must not exceed 1000 characters")
    @Column(name = "hospital_notes", length = 1000)
    private String hospitalNotes;

    @Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = RequestStatus.PENDING;
        }
        if (this.unitsFulfilled == null) {
            this.unitsFulfilled = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "BloodRequest{id=" + id + ", bloodGroup=" + bloodGroup + ", status=" + status + "}";
    }
}
