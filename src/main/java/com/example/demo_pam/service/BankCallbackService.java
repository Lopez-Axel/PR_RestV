package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.BankCallbackRequest;
import org.springframework.stereotype.Service;

// Orquesta el webhook del banco: verifica la firma y delega el asentamiento en el pago
@Service
public class BankCallbackService {

    private final BankPaymentProvider bankPaymentProvider;
    private final PaymentService paymentService;

    public BankCallbackService(BankPaymentProvider bankPaymentProvider, PaymentService paymentService) {
        this.bankPaymentProvider = bankPaymentProvider;
        this.paymentService = paymentService;
    }

    // El banco no trae nuestro JWT: la ruta es pública y la seguridad la aporta la verificación de firma
    public void handleCallback(BankCallbackRequest request, String signature) {
        bankPaymentProvider.verifyCallback(signature, "callback:" + request.transactionId() + ":" + request.outcome());
        paymentService.settleByBankTransaction(request.transactionId(), request.outcome(), request.bankReference());
    }
}