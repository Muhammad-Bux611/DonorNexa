package com.donornexa.messaging.entity;

import com.donornexa.bloodrequest.entity.BloodRequest;
import com.donornexa.donor.entity.Donor;
import com.donornexa.hospital.entity.Hospital;


import com.donornexa.common.enums.ConversationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a secure, dedicated communication channel between an authorized donor
 * and a hospital concerning a specific blood request coordination.
 */
@Getter
@Setter
@Entity
@Table(
    name = "conversations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_conversations_donor_hospital_request",
            columnNames = {"donor_id", "hospital_id", "blood_request_id"}
        )
    },
    indexes = {
        @Index(name = "idx_conversations_donor_id",         columnList = "donor_id"),
        @Index(name = "idx_conversations_hospital_id",      columnList = "hospital_id"),
        @Index(name = "idx_conversations_blood_request_id", columnList = "blood_request_id"),
        @Index(name = "idx_conversations_status",           columnList = "status"),
        @Index(name = "idx_conversations_last_message_at",  columnList = "last_message_at")
    }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Donor participant is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_conversations_donor"))
    private Donor donor;

    @NotNull(message = "Hospital participant is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_conversations_hospital"))
    private Hospital hospital;

    @NotNull(message = "Associated blood request is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_conversations_blood_request"))
    private BloodRequest bloodRequest;

    @NotNull(message = "Conversation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ConversationStatus.ACTIVE;
        }
        if (this.lastMessageAt == null) {
            this.lastMessageAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
