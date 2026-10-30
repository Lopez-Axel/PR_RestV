package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Dish;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DishDTO(
        UUID id,
        String name,
        String description,
        String categoryName,
        UUID categoryId,
        BigDecimal sellingPrice,
        Boolean available,
        String imageUrl,
        BigDecimal productionCost,
        BigDecimal suggestedSellingPrice,
        String status,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static DishDTO from(Dish dish) {
        return new DishDTO(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getCategory().getName(),
                dish.getCategory().getId(),
                dish.getSellingPrice(),
                dish.getAvailable(),
                dish.getImageUrl(),
                dish.getProductionCost(),
                null,
                null,
                dish.getCreatedAt(),
                dish.getUpdatedAt(),
                AuditUserDTO.from(dish.getCreatedBy()),
                AuditUserDTO.from(dish.getUpdatedBy())
        );
    }
}