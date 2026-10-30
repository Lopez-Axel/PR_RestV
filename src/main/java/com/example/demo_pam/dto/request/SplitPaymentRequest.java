package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

// División de una orden SERVED en N partes (una por persona), cada una con sus líneas y método
public record SplitPaymentRequest(
        @NotNull(message = "Las partes son obligatorias")
        @NotEmpty(message = "Debe haber al menos una parte")
        @Valid
        List<Part> parts
) {

    public record Part(
            @NotBlank(message = "El nombre de la persona es obligatorio")
            String personName,

            @NotNull(message = "El método de pago es obligatorio")
            Payment.PaymentMethod paymentMethod,

            @NotNull(message = "Las líneas son obligatorias")
            @NotEmpty(message = "Cada parte debe cubrir al menos una línea")
            List<UUID> orderDetailIds
    ) {
    }
}