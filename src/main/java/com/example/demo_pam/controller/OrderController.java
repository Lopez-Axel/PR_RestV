package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.OrderCreateRequest;
import com.example.demo_pam.dto.request.OrderDetailCreateRequest;
import com.example.demo_pam.dto.request.OrderStatusUpdateRequest;
import com.example.demo_pam.dto.response.OrderDetailResponse;
import com.example.demo_pam.dto.response.OrderResponse;
import com.example.demo_pam.security.AppUserPrincipal;
import com.example.demo_pam.service.OrderService;
import com.example.demo_pam.service.OrderStatusSseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderStatusSseService orderStatusSseService;

    public OrderController(OrderService orderService, OrderStatusSseService orderStatusSseService) {
        this.orderService = orderService;
        this.orderStatusSseService = orderStatusSseService;
    }

    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PostMapping
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request,
                                @AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.createOrder(request, principal.getUser());
    }

    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @PostMapping("/{orderId}/details")
    public OrderDetailResponse addDetail(@PathVariable UUID orderId,
                                         @Valid @RequestBody OrderDetailCreateRequest request,
                                         @AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.addDetail(orderId, request, principal.getUser());
    }

    @GetMapping
    public Page<OrderResponse> findAll(@RequestParam(required = false) UUID tableId,
                                       @PageableDefault(size = 20, sort = "dateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return tableId != null
                ? orderService.listByTable(tableId, pageable)
                : orderService.findAll(pageable);
    }

    // Pedidos del usuario autenticado (el mozo): el user_id se toma del JWT, no se envía en la URL
    @PreAuthorize("hasAnyAuthority('WAITER', 'ADMIN')")
    @GetMapping("/my")
    public Page<OrderResponse> findMyOrders(@PageableDefault(size = 20, sort = "dateTime", direction = Sort.Direction.DESC) Pageable pageable,
                                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.listByUser(principal.getUser().getId(), pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id) {
        return orderService.findById(id);
    }

    // Cualquier rol de operación actualiza el estado; el pago es el único que llega a CLOSED
    @GetMapping("/{orderId}/details")
    public List<OrderDetailResponse> findDetails(@PathVariable UUID orderId) {
        return orderService.listDetails(orderId);
    }

    @PreAuthorize("hasAnyAuthority('WAITER', 'CHEF', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody OrderStatusUpdateRequest request,
                                      @AuthenticationPrincipal AppUserPrincipal principal) {
        return orderService.updateStatus(id, request, principal.getUser());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // Suscripción SSE: la cocina recibe cada pedido y cambio de estado sin hacer polling
    @GetMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter statusStream() {
        return orderStatusSseService.subscribe();
    }
}