package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMenuDishRequest(
        @NotNull(message = "El plato es obligatorio")
        UUID dishId
) {
}