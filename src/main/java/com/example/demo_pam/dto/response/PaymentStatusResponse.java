package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.PaymentStatus;

import java.util.UUID;

public record PaymentStatusResponse(
        UUID id,
        String name
) {

    public static PaymentStatusResponse from(PaymentStatus status) {
        return new PaymentStatusResponse(status.getId(), status.getName().name());
    }
}