package com.lakeserl.user_service.model.dto.request;

import com.lakeserl.user_service.security.validate.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
