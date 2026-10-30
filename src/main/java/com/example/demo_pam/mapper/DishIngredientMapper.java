package com.example.demo_pam.mapper;

import com.example.demo_pam.dto.response.DishIngredientDTO;
import com.example.demo_pam.entity.DishIngredient;

import java.util.List;

public final class DishIngredientMapper {

    private DishIngredientMapper() {
    }

    public static DishIngredientDTO toResponse(DishIngredient dishIngredient) {
        return DishIngredientDTO.from(dishIngredient);
    }

    public static List<DishIngredientDTO> toResponseList(List<DishIngredient> dishIngredients) {
        return dishIngredients.stream().map(DishIngredientMapper::toResponse).toList();
    }
}