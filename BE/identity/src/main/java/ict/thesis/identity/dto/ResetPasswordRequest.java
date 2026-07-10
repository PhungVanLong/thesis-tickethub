package ict.thesis.identity.dto;

import jakarta.validation.constraints.NotBlank;

// TODO: Phát triển tính năng đặt lại mật khẩu sau
public record ResetPasswordRequest(
    @NotBlank(message = "Token or OTP is required")
    String token,
    
    @NotBlank(message = "New password is required")
    String newPassword
) {
}
