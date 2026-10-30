package com.example.demo_pam.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CostAnalysisDTO(
        UUID dishId,
        String dishName,
        BigDecimal productionCost,
        BigDecimal suggestedSellingPrice,
        BigDecimal currentSellingPrice,
        String status
) {
}