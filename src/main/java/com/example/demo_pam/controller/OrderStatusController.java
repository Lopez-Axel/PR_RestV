package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.OrderStatusRequest;
import com.example.demo_pam.dto.response.OrderStatusResponse;
import com.example.demo_pam.service.OrderStatusService;
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
@RequestMapping("/order-statuses")
public class OrderStatusController {

    private final OrderStatusService orderStatusService;

    public OrderStatusController(OrderStatusService orderStatusService) {
        this.orderStatusService = orderStatusService;
    }

    @GetMapping
    public List<OrderStatusResponse> findAll() {
        return orderStatusService.findAll();
    }

    @GetMapping("/{id}")
    public OrderStatusResponse findById(@PathVariable UUID id) {
        return orderStatusService.findById(id);
    }

    // Catálogo de configuración: solo ADMIN crea estados y define su color
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public OrderStatusResponse create(@Valid @RequestBody OrderStatusRequest request) {
        return orderStatusService.create(request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public OrderStatusResponse update(@PathVariable UUID id, @Valid @RequestBody OrderStatusRequest request) {
        return orderStatusService.update(id, request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}