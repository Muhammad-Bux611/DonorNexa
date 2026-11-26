package com.donornexa.audit.entity;

import com.donornexa.user.entity.User;


import com.donornexa.common.enums.AuditAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records important system activities for security and compliance auditing.
 * The actor (user) can be null for system-generated events.
 * Audit records must never be casually deleted.
 */
@Getter
@Setter
@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_logs_actor_id",    columnList = "actor_id"),
        @Index(name = "idx_audit_logs_action",      columnList = "action"),
        @Index(name = "idx_audit_logs_entity_name", columnList = "entity_name"),
        @Index(name = "idx_audit_logs_created_at",  columnList = "created_at")
    }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who performed the action. Nullable for system-generated events.
     * No cascade â€” audit logs must be independent of user lifecycle.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id",
                foreignKey = @ForeignKey(name = "fk_audit_logs_actor"))
    private User actor;

    @NotNull(message = "Audit action is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 35)
    private AuditAction action;

    @Size(max = 100, message = "Entity name must not exceed 100 characters")
    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Size(max = 100, message = "Entity ID must not exceed 100 characters")
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Size(max = 1000, message = "Old value must not exceed 1000 characters")
    @Column(name = "old_value", length = 1000)
    private String oldValue;

    @Size(max = 1000, message = "New value must not exceed 1000 characters")
    @Column(name = "new_value", length = 1000)
    private String newValue;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Column(name = "description", length = 2000)
    private String description;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "AuditLog{id=" + id + ", action=" + action + ", entityName='" + entityName + "'}";
    }
}
