package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.UpdateSeatRequest;
import com.example.demo_pam.dto.response.SeatResponse;
import com.example.demo_pam.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurantTable/{tableId}/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public List<SeatResponse> findByTableId(@PathVariable UUID tableId) {
        return seatService.findByTableId(tableId);
    }

    // Marca una silla como ocupada o libre (ADMIN o WAITER), notificando por SSE
    @PreAuthorize("hasAnyAuthority('ADMIN', 'WAITER')")
    @PatchMapping("/{seatId}")
    public SeatResponse updateOccupied(@PathVariable UUID tableId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request) {
        return seatService.updateOccupied(tableId, seatId, request.occupied());
    }
}
