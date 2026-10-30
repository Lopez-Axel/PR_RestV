package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Recibo de un pago confirmado; el encabezado del local lo superpone el frontend al imprimir
public record PaymentReceiptResponse(
        String folio,
        UUID paymentId,
        UUID orderId,
        Integer tableNumber,
        String payeeName,
        Payment.PaymentMethod paymentMethod,
        String status,
        BigDecimal amount,
        String bankReference,
        Instant paymentDate,
        String confirmedBy,
        List<PaymentDetailResponse> items
) {

    public static PaymentReceiptResponse from(Payment payment) {
        return new PaymentReceiptResponse(
                payment.getReceiptNumber() == null ? null : "REC-%06d".formatted(payment.getReceiptNumber()),
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getTable().getNumber(),
                payment.getPayeeName(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus().getName().name(),
                payment.getAmount(),
                payment.getBankReference(),
                payment.getPaymentDate(),
                payment.getUpdatedBy() == null ? null
                        : (payment.getUpdatedBy().getFirstName() + " "
                        + payment.getUpdatedBy().getLastName()).trim(),
                payment.getPaymentDetails() == null ? null
                        : payment.getPaymentDetails().stream().map(PaymentDetailResponse::from).toList()
        );
    }
}