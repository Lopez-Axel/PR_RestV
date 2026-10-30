package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.PaymentStatusRequest;
import com.example.demo_pam.dto.response.PaymentStatusResponse;
import com.example.demo_pam.service.PaymentStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payment-statuses")
public class PaymentStatusController {

    private final PaymentStatusService paymentStatusService;

    public PaymentStatusController(PaymentStatusService paymentStatusService) {
        this.paymentStatusService = paymentStatusService;
    }

    @GetMapping
    public List<PaymentStatusResponse> findAll() {
        return paymentStatusService.findAll();
    }

    @GetMapping("/{id}")
    public PaymentStatusResponse findById(@PathVariable UUID id) {
        return paymentStatusService.findById(id);
    }

    // Catálogo de configuración: solo ADMIN crea estados de pago
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public PaymentStatusResponse create(@Valid @RequestBody PaymentStatusRequest request) {
        return paymentStatusService.create(request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public PaymentStatusResponse update(@PathVariable UUID id, @Valid @RequestBody PaymentStatusRequest request) {
        return paymentStatusService.update(id, request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        paymentStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}