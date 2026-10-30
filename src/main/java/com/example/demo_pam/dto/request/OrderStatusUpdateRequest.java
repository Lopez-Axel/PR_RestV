package com.example.demo_pam.dto.request;

import com.example.demo_pam.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "El estado es obligatorio")
        OrderStatus.OrderStatusName statusName
) {
}