package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Ingredient;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientDTO(
        UUID id,
        String name,
        UUID unitId,
        String unitName,
        String unitAbbreviation,
        BigDecimal price,
        BigDecimal currentStock,
        BigDecimal minimumStock
) {

    public static IngredientDTO from(Ingredient ingredient) {
        return new IngredientDTO(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnit().getId(),
                ingredient.getUnit().getName(),
                ingredient.getUnit().getAbbreviation(),
                ingredient.getPrice(),
                ingredient.getCurrentStock(),
                ingredient.getMinimumStock()
        );
    }
}