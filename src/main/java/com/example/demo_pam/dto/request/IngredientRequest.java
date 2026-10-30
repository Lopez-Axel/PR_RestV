package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotNull(message = "La unidad de medida es obligatoria")
        UUID unitId,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio tiene hasta 2 decimales")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El stock tiene hasta 2 decimales")
        BigDecimal currentStock,

        @DecimalMin(value = "0.0", message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El stock mínimo tiene hasta 2 decimales")
        BigDecimal minimumStock
) {
}