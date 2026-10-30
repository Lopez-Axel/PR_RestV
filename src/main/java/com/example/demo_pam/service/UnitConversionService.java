package com.example.demo_pam.service;

import com.example.demo_pam.entity.MeasurementUnit;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;      // <-- añadido
import java.util.Objects;          // <-- añadido

@Service
public class UnitConversionService {

    public BigDecimal convert(BigDecimal quantity, MeasurementUnit from, MeasurementUnit to) {
        Objects.requireNonNull(quantity, "quantity no puede ser null");
        Objects.requireNonNull(from, "from no puede ser null");
        Objects.requireNonNull(to, "to no puede ser null");

        if (from.getUnitType() != to.getUnitType()) {
            throw new IllegalArgumentException(
                "No se puede convertir entre unidades de distinto tipo: "
                + from.getName() + " (" + from.getUnitType() + ") y "
                + to.getName() + " (" + to.getUnitType() + ")");
        }

        if (from.equals(to)) {
            return quantity;
        }

        BigDecimal fromFactor = from.getConversionFactorToBase();
        BigDecimal toFactor   = to.getConversionFactorToBase();

        if (fromFactor == null || fromFactor.signum() == 0) {
            throw new IllegalArgumentException(
                "La unidad " + from.getName() + " tiene factor de conversión inválido");
        }
        if (toFactor == null || toFactor.signum() == 0) {
            throw new IllegalArgumentException(
                "La unidad " + to.getName() + " tiene factor de conversión inválido");
        }

        return quantity.multiply(fromFactor)
                       .divide(toFactor, MathContext.DECIMAL128);
    }
}
