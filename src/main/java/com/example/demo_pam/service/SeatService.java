package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.RestaurantTableResponse;
import com.example.demo_pam.dto.response.SeatResponse;
import com.example.demo_pam.entity.RestaurantTable;
import com.example.demo_pam.entity.Seat;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final TableStatusSseService sseService;

    public SeatService(SeatRepository seatRepository, TableStatusSseService sseService) {
        this.seatRepository = seatRepository;
        this.sseService = sseService;
    }

    public List<SeatResponse> findByTableId(UUID tableId) {
        return seatRepository.findByTableId(tableId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    // Cambia la ocupación de una silla de la mesa indicada y notifica por SSE
    @Transactional
    public SeatResponse updateOccupied(UUID tableId, UUID seatId, Boolean occupied) {
        Seat seat = findById(seatId);
        if (!seat.getTable().getId().equals(tableId)) {
            throw new ResourceNotFoundException(
                    "La silla " + seatId + " no pertenece a la mesa " + tableId);
        }
        seat.setOccupied(occupied);
        Seat saved = seatRepository.save(seat);
        sseService.broadcast(RestaurantTableResponse.from(seat.getTable()));
        return SeatResponse.from(saved);
    }

    public Seat findById(UUID id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Silla no encontrada: " + id));
    }

    // Genera las sillas 1..capacity de una mesa, usado al crear la mesa o al aumentar su capacidad
    @Transactional
    public void generateSeatsForTable(RestaurantTable table, int capacity) {
        int start = 1;
        List<Seat> existing = seatRepository.findByTableId(table.getId());
        if (!existing.isEmpty()) {
            int max = existing.stream().mapToInt(Seat::getNumber).max().orElse(0);
            start = max + 1;
        }
        for (int i = start; i <= capacity; i++) {
            Seat seat = new Seat();
            seat.setTable(table);
            seat.setNumber(i);
            seat.setOccupied(false);
            seatRepository.save(seat);
        }
    }

    // Reduce la capacidad: elimina las sillas con número mayor al nuevo valor, si ninguna está ocupada
    @Transactional
    public void removeSeatsBeyondCapacity(RestaurantTable table, int newCapacity) {
        List<Seat> removable = seatRepository.findByTableId(table.getId()).stream()
                .filter(seat -> seat.getNumber() > newCapacity)
                .toList();
        boolean occupiedRemovable = removable.stream().anyMatch(Seat::getOccupied);
        if (occupiedRemovable) {
            throw new BusinessRuleViolationException(
                    "No se puede reducir la capacidad: hay sillas ocupadas con número mayor a " + newCapacity);
        }
        seatRepository.deleteAll(removable);
    }
}
