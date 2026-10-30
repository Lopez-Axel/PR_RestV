package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.Size;

public record PaymentConfirmRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String payeeName,

        String bankReference
) {
}