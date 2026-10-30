package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        Payment.PaymentMethod paymentMethod,
        String qrCode,
        String status,
        Instant expiresAt,
        String bankTransactionId,
        String bankReference,
        Instant paymentDate,
        String payeeName,
        String receiptNumber,
        List<PaymentDetailResponse> items,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getQrCode() == null ? null : "data:image/png;base64," + payment.getQrCode(),
                payment.getPaymentStatus().getName().name(),
                payment.getExpiresAt(),
                payment.getBankTransactionId(),
                payment.getBankReference(),
                payment.getPaymentDate(),
                payment.getPayeeName(),
                payment.getReceiptNumber() == null ? null : "REC-%06d".formatted(payment.getReceiptNumber()),
                payment.getPaymentDetails() == null ? null
                        : payment.getPaymentDetails().stream().map(PaymentDetailResponse::from).toList(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                AuditUserDTO.from(payment.getCreatedBy()),
                AuditUserDTO.from(payment.getUpdatedBy())
        );
    }
}