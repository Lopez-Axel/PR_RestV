package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "measurement_unit")
@Getter
@Setter
public class MeasurementUnit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String abbreviation;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private UnitType unitType;

    @Column(name = "conversion_factor_to_base", nullable = false, precision = 19, scale = 10)
    private BigDecimal conversionFactorToBase;

    public enum UnitType {
        WEIGHT, VOLUME, COUNT
    }
}