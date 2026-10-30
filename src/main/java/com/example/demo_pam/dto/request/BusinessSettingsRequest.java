package com.example.demo_pam.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BusinessSettingsRequest(
        @NotNull(message = "El margen por defecto es obligatorio")
        @Digits(integer = 10, fraction = 2, message = "El margen tiene hasta 2 decimales")
        BigDecimal defaultMarginPercentage,

        @NotNull(message = "El margen mínimo es obligatorio")
        @DecimalMin(value = "0.0", message = "El margen mínimo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El margen tiene hasta 2 decimales")
        BigDecimal minMarginPercentage,

        @NotNull(message = "El margen máximo es obligatorio")
        @DecimalMin(value = "0.0", message = "El margen máximo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El margen tiene hasta 2 decimales")
        BigDecimal maxMarginPercentage,

        @Min(value = 0, message = "El contador de recibos no puede ser negativo")
        Long receiptCounter
) {
}