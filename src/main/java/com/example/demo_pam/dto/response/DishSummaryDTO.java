package com.example.demo_pam.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DishSummaryDTO(
        UUID dishId,
        String dishName,
        String description,
        String categoryName,
        Boolean available,
        String imageUrl,
        BigDecimal productionCost,
        BigDecimal suggestedSellingPrice,
        BigDecimal sellingPrice,
        BigDecimal marginPercent,
        String status,
        List<DishIngredientDTO> ingredients,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {
}