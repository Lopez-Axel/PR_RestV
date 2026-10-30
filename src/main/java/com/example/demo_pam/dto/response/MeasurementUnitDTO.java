package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.MeasurementUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record MeasurementUnitDTO(
        UUID id,
        String name,
        String abbreviation,
        String unitType,
        BigDecimal conversionFactorToBase
) {

    public static MeasurementUnitDTO from(MeasurementUnit unit) {
        return new MeasurementUnitDTO(
                unit.getId(),
                unit.getName(),
                unit.getAbbreviation(),
                unit.getUnitType().name(),
                unit.getConversionFactorToBase()
        );
    }
}