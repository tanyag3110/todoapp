package com.example.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotBlank
    @Email
    private String email;

    private String role; // CUSTOMER, INTERNAL, APPLICATION

    @NotBlank(message = "CAPTCHA is required")
    private String captcha;
}
