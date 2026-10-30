package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dish_ingredient", uniqueConstraints = {
        // Evita duplicar un ingrediente dentro de un mismo plato sin usar clave compuesta
        @UniqueConstraint(name = "uk_dish_ingredient", columnNames = {"dish_id", "ingredient_id"})
})
@Getter
@Setter
// Restringe todas las consultas a filas no borradas (borrado lógico unificado)
@SQLRestriction("deleted = false")
public class DishIngredient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "estimated_quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal estimatedQuantity;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false)
    private MeasurementUnit unit;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false NOT NULL")
    private Boolean deleted = false;
}