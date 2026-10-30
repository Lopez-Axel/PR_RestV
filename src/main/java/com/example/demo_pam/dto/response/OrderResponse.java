package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Order;
import com.example.demo_pam.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID tableId,
        Integer tableNumber,
        UUID waiterId,
        String waiterName,
        String orderStatusName,
        String colorHex,
        LocalDateTime dateTime,
        String notes,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static OrderResponse from(Order order) {
        User user = order.getUser();
        String waiterName = user == null ? null
                : (user.getFirstName() + " " + user.getLastName()).trim();
        BigDecimal total = order.getOrderDetails().stream()
                .map(com.example.demo_pam.entity.OrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(
                order.getId(),
                order.getTable().getId(),
                order.getTable().getNumber(),
                user == null ? null : user.getId(),
                waiterName,
                order.getOrderStatus().getName().name(),
                order.getOrderStatus().getColorHex(),
                order.getDateTime(),
                order.getNotes(),
                total,
                order.getCreatedAt(),
                order.getUpdatedAt(),
                AuditUserDTO.from(order.getCreatedBy()),
                AuditUserDTO.from(order.getUpdatedBy())
        );
    }
}