package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.PaymentDetail;

import java.math.BigDecimal;
import java.util.UUID;

// Línea de la orden cubierta por un pago (parte de una persona), para el recibo y la vista del cobro
public record PaymentDetailResponse(
        UUID orderDetailId,
        UUID dishId,
        String dishName,
        Long quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {

    public static PaymentDetailResponse from(PaymentDetail paymentDetail) {
        return new PaymentDetailResponse(
                paymentDetail.getOrderDetail().getId(),
                paymentDetail.getOrderDetail().getDish().getId(),
                paymentDetail.getOrderDetail().getDish().getName(),
                paymentDetail.getOrderDetail().getQuantity(),
                paymentDetail.getOrderDetail().getUnitPrice(),
                paymentDetail.getSubtotal()
        );
    }
}