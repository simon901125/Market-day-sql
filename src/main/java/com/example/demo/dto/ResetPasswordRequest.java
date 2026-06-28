package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Password reset request")
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "One-time token returned after email code verification")
    private String resetToken;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters and contain letters and numbers")
    @Schema(
            description = "New password with at least 8 characters, including letters and numbers",
            example = "NewPassword123")
    private String password;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
