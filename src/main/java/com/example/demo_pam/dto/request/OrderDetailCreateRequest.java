package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderDetailCreateRequest(
        @NotNull(message = "El plato es obligatorio")
        UUID dishId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Long quantity
) {
}