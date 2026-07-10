package ict.thesis.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// TODO: Phát triển tính năng gửi email khôi phục mật khẩu sau
public record ForgotPasswordRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {
}
