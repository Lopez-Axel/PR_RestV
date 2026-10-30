package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.Payment;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull(message = "La orden es obligatoria")
        UUID orderId,

        @NotNull(message = "El método de pago es obligatorio")
        Payment.PaymentMethod paymentMethod
) {
}