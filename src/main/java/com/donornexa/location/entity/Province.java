package com.donornexa.location.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an administrative province or region.
 * Cities are grouped under provinces, and donors/hospitals
 * are associated with provinces for location-based filtering.
 */
@Getter
@Setter
@Entity
@Table(
    name = "provinces",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_provinces_name", columnNames = "name")
    },
    indexes = {
        @Index(name = "idx_provinces_name", columnList = "name")
    }
)
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Province name is required")
    @Size(max = 100, message = "Province name must not exceed 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Province{id=" + id + ", name='" + name + "'}";
    }
}
