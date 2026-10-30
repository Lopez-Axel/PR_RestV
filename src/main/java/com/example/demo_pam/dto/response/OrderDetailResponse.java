package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.OrderDetail;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        UUID orderId,
        UUID dishId,
        String dishName,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {

    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(
                detail.getId(),
                detail.getOrder().getId(),
                detail.getDish().getId(),
                detail.getDish().getName(),
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getSubtotal()
        );
    }
}