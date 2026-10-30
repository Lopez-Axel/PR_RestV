package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.RestaurantTableRequest;
import com.example.demo_pam.dto.request.UpdateTableStatusRequest;
import com.example.demo_pam.dto.response.RestaurantTableResponse;
import com.example.demo_pam.dto.response.TableStatusHistoryResponse;
import com.example.demo_pam.security.AppUserPrincipal;
import com.example.demo_pam.service.RestaurantTableService;
import com.example.demo_pam.service.TableStatusSseService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurantTable")
public class RestaurantTableController {

    private final RestaurantTableService restaurantTableService;
    private final TableStatusSseService sseService;

    public RestaurantTableController(RestaurantTableService restaurantTableService,
                                     TableStatusSseService sseService) {
        this.restaurantTableService = restaurantTableService;
        this.sseService = sseService;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public RestaurantTableResponse create(@Valid @RequestBody RestaurantTableRequest request) {
        return restaurantTableService.create(request);
    }

    @GetMapping
    public List<RestaurantTableResponse> findAll() {
        return restaurantTableService.findAll();
    }

    @GetMapping("/{id}")
    public RestaurantTableResponse findById(@PathVariable UUID id) {
        return restaurantTableService.findById(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public RestaurantTableResponse update(@PathVariable UUID id, @Valid @RequestBody RestaurantTableRequest request) {
        return restaurantTableService.update(id, request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        restaurantTableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Actualiza el estado y registra el histórico con el usuario autenticado (ADMIN o WAITER)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WAITER')")
    @PutMapping("/{id}/status")
    public RestaurantTableResponse updateStatus(@PathVariable UUID id,
            @Valid @RequestBody UpdateTableStatusRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return restaurantTableService.updateStatus(id, request, principal.getUser());
    }

    // Suscripción SSE: el cliente recibe cada cambio de estado sin hacer polling
    @GetMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter statusStream() {
        return sseService.subscribe();
    }

    @GetMapping("/{id}/history")
    public List<TableStatusHistoryResponse> findHistory(@PathVariable UUID id) {
        return restaurantTableService.findHistory(id);
    }
}
