package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BankCallbackRequest(
        @NotBlank(message = "El transactionId es obligatorio")
        String transactionId,

        @NotBlank(message = "El outcome (SUCCESS|FAILED) es obligatorio")
        String outcome,

        String bankReference
) {
}