package com.example.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;

@Data
public class LoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
