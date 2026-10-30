package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DishIngredientRequest(
        @NotNull(message = "El ingrediente es obligatorio")
        UUID ingredientId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0", message = "La cantidad no puede ser negativa")
        @Digits(integer = 10, fraction = 6, message = "La cantidad tiene hasta 6 decimales")
        BigDecimal estimatedQuantity,

        @NotNull(message = "La unidad de medida es obligatoria")
        UUID unitId
) {
}