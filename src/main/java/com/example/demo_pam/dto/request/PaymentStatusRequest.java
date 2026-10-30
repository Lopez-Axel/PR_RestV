package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(
        @NotNull(message = "El nombre es obligatorio")
        PaymentStatus.PaymentStatusName name
) {
}