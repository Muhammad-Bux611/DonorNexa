package com.donornexa.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a configurable platform setting stored in the database.
 * Allows administrators to customize runtime parameters like donor cooldown days.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "settings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_settings_setting_key", columnNames = "setting_key")
    },
    indexes = {
        @Index(name = "idx_settings_setting_key", columnList = "setting_key")
    }
)
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Setting key is required")
    @Size(max = 100, message = "Setting key must not exceed 100 characters")
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @NotBlank(message = "Setting value is required")
    @Size(max = 255, message = "Setting value must not exceed 255 characters")
    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Setting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
