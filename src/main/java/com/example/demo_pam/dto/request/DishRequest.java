package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DishRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "La descripción es obligatoria")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        UUID categoryId,

        @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio de venta tiene hasta 2 decimales")
        BigDecimal sellingPrice,

        @DecimalMin(value = "0.0", message = "El costo de producción no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo tiene hasta 2 decimales")
        BigDecimal productionCost,

        Boolean available,

        String imageUrl
) {
}