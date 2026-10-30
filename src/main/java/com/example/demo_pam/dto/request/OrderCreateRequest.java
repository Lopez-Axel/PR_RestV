package com.example.demo_pam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull(message = "La mesa es obligatoria")
        UUID tableId,

        String notes,

        @NotEmpty(message = "El pedido debe tener al menos un plato")
        List<@Valid OrderDetailCreateRequest> details
) {
}