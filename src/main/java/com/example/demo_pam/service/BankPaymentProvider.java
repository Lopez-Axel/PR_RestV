package com.example.demo_pam.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface BankPaymentProvider {

    // Solicita un QR dinámico para una transferencia y devuelve la imagen Base64 + la referencia del banco
    DynamicQr createDynamicQr(UUID paymentId, BigDecimal amount);

    // Valida la firma de un callback del banco; lanza excepción si no coincide
    void verifyCallback(String signature, String payload);

    record DynamicQr(String imageBase64, String transactionId) {
    }
}