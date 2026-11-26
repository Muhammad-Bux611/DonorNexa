package com.donornexa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for administrator secure password change.
 */
@Getter
@Setter
public class AdminPasswordChangeDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters long")
    private String newPassword;

    @NotBlank(message = "Please confirm the new password")
    private String confirmPassword;
}
