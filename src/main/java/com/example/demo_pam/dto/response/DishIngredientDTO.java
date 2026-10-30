package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.DishIngredient;

import java.math.BigDecimal;
import java.util.UUID;

public record DishIngredientDTO(
        UUID id,
        UUID dishId,
        UUID ingredientId,
        String ingredientName,
        BigDecimal estimatedQuantity,
        UUID unitId,
        String unitName,
        String unitAbbreviation,
        Boolean available
) {

    public static DishIngredientDTO from(DishIngredient dishIngredient) {
        return new DishIngredientDTO(
                dishIngredient.getId(),
                dishIngredient.getDish().getId(),
                dishIngredient.getIngredient().getId(),
                dishIngredient.getIngredient().getName(),
                dishIngredient.getEstimatedQuantity(),
                dishIngredient.getUnit().getId(),
                dishIngredient.getUnit().getName(),
                dishIngredient.getUnit().getAbbreviation(),
                dishIngredient.getAvailable()
        );
    }
}