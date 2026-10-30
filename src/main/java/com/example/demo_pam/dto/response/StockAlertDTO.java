package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.StockAlert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockAlertDTO(
        UUID id,
        UUID ingredientId,
        String ingredientName,
        String unitAbbreviation,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        String status,
        Instant detectedAt,
        Instant notifiedAt,
        Instant resolvedAt
) {

    public static StockAlertDTO from(StockAlert alert) {
        Ingredient ingredient = alert.getIngredient();
        return new StockAlertDTO(
                alert.getId(),
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnit().getAbbreviation(),
                ingredient.getCurrentStock(),
                ingredient.getMinimumStock(),
                alert.getStatus().name(),
                alert.getDetectedAt(),
                alert.getNotifiedAt(),
                alert.getResolvedAt()
        );
    }
}
