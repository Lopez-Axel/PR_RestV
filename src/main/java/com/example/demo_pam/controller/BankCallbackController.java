package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.BankCallbackRequest;
import com.example.demo_pam.service.BankCallbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
// Ruta pública que el banco usa para notificarnos el resultado de la transferencia
@RequestMapping("/api/bank/callback")
public class BankCallbackController {

    private final BankCallbackService bankCallbackService;

    public BankCallbackController(BankCallbackService bankCallbackService) {
        this.bankCallbackService = bankCallbackService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@Valid @RequestBody BankCallbackRequest request,
                                                       @RequestHeader(name = "X-Bank-Signature", required = false) String signature) {
        bankCallbackService.handleCallback(request, signature);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}