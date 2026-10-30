package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MeasurementUnitRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String name,

        @NotBlank(message = "La abreviación es obligatoria")
        @Size(max = 10, message = "La abreviación no puede superar los 10 caracteres")
        String abbreviation,

        @NotNull(message = "El tipo de unidad es obligatorio")
        MeasurementUnit.UnitType unitType,

        @NotNull(message = "El factor de conversión es obligatorio")
        @DecimalMin(value = "0.0", message = "El factor de conversión no puede ser negativo")
        @Digits(integer = 10, fraction = 10, message = "El factor tiene hasta 10 decimales")
        BigDecimal conversionFactorToBase
) {
}