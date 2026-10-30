package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestaurantTableRequest(
        @NotNull(message = "El número de mesa es obligatorio")
        @Positive(message = "El número de mesa debe ser positivo")
        Integer number,

        @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser positiva")
        Integer capacity
) {
}
