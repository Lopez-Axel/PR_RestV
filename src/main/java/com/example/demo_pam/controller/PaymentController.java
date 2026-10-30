package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.PaymentConfirmRequest;
import com.example.demo_pam.dto.request.PaymentCreateRequest;
import com.example.demo_pam.dto.request.SplitPaymentRequest;
import com.example.demo_pam.dto.response.PaymentReceiptResponse;
import com.example.demo_pam.dto.response.PaymentResponse;
import com.example.demo_pam.entity.PaymentStatus;
import com.example.demo_pam.security.AppUserPrincipal;
import com.example.demo_pam.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Crea la parte "el resto": cubre las líneas aún no cobradas de la orden SERVED
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PostMapping
    public PaymentResponse create(@Valid @RequestBody PaymentCreateRequest request,
                                  @AuthenticationPrincipal AppUserPrincipal principal) {
        return paymentService.createPayment(request, principal.getUser());
    }

    // Divide la cuenta en una parte por persona, cada una con sus líneas y su QR propio
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PostMapping("/split/{orderId}")
    public List<PaymentResponse> split(@PathVariable UUID orderId,
                                       @Valid @RequestBody SplitPaymentRequest request,
                                       @AuthenticationPrincipal AppUserPrincipal principal) {
        return paymentService.createSplit(orderId, request, principal.getUser());
    }

    @GetMapping
    public Page<PaymentResponse> findAll(@RequestParam(required = false) PaymentStatus.PaymentStatusName status,
                                         @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return paymentService.listByStatus(status, pageable);
        }
        return paymentService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable UUID id) {
        return paymentService.findById(id);
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> findByOrder(@PathVariable UUID orderId) {
        return paymentService.listByOrder(orderId);
    }

    // Confirma efectivo/QR estático (WAITER o ADMIN); las transferencias se confirman por callback del banco
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PatchMapping("/{id}/confirm")
    public PaymentResponse confirm(@PathVariable UUID id,
                                   @Valid @RequestBody PaymentConfirmRequest request,
                                   @AuthenticationPrincipal AppUserPrincipal principal) {
        return paymentService.confirmPayment(id, request.payeeName(), request.bankReference(), principal.getUser());
    }

    // Marca un intento como fallido: queda el registro pero el QR deja de ser válido
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PatchMapping("/{id}/fail")
    public PaymentResponse fail(@PathVariable UUID id,
                                @AuthenticationPrincipal AppUserPrincipal principal) {
        return paymentService.failPayment(id, principal.getUser());
    }

    // Recibo imprimible en JSON (folio secuencial); el encabezado del local lo agrega el frontend
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @GetMapping("/{id}/receipt")
    public PaymentReceiptResponse receipt(@PathVariable UUID id) {
        return paymentService.getReceipt(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        paymentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}