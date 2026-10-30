package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.OrderStatus;

import java.util.UUID;

public record OrderStatusResponse(
        UUID id,
        String name,
        String colorHex
) {

    public static OrderStatusResponse from(OrderStatus status) {
        return new OrderStatusResponse(status.getId(), status.getName().name(), status.getColorHex());
    }
}