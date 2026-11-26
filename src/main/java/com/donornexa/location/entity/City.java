package com.donornexa.location.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a city within a province.
 * A composite unique constraint prevents duplicate city names
 * within the same province.
 */
@Getter
@Setter
@Entity
@Table(
    name = "cities",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cities_name_province", columnNames = {"name", "province_id"})
    },
    indexes = {
        @Index(name = "idx_cities_province_id", columnList = "province_id"),
        @Index(name = "idx_cities_name",        columnList = "name")
    }
)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * The province this city belongs to.
     * LAZY fetch avoids unnecessary province data loading.
     */
    @NotNull(message = "Province is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cities_province"))
    private Province province;

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
        return "City{id=" + id + ", name='" + name + "', province=" + province + "}";
    }
}
