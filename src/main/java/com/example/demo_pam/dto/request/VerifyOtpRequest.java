package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email con formato inválido")
        String email,

        @NotBlank(message = "El código es obligatorio")
        String code
) {
}
