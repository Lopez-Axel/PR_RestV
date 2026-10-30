package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.RestaurantTableRequest;
import com.example.demo_pam.dto.request.UpdateTableStatusRequest;
import com.example.demo_pam.dto.response.RestaurantTableResponse;
import com.example.demo_pam.dto.response.TableStatusHistoryResponse;
import com.example.demo_pam.entity.RestaurantTable;
import com.example.demo_pam.entity.TableStatus;
import com.example.demo_pam.entity.TableStatusHistory;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.RestaurantTableRepository;
import com.example.demo_pam.repository.TableStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RestaurantTableService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final TableStatusHistoryRepository historyRepository;
    private final TableStatusService tableStatusService;
    private final SeatService seatService;
    private final TableStatusUpdateService tableStatusUpdateService;

    public RestaurantTableService(RestaurantTableRepository restaurantTableRepository,
                                  TableStatusHistoryRepository historyRepository,
                                  TableStatusService tableStatusService,
                                  SeatService seatService,
                                  TableStatusUpdateService tableStatusUpdateService) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.historyRepository = historyRepository;
        this.tableStatusService = tableStatusService;
        this.seatService = seatService;
        this.tableStatusUpdateService = tableStatusUpdateService;
    }

    // Crea la mesa con estado AVAILABLE y genera sus sillas dentro de la misma transacción
    @Transactional
    public RestaurantTableResponse create(RestaurantTableRequest request) {
        RestaurantTable table = new RestaurantTable();
        table.setNumber(request.number());
        table.setCapacity(request.capacity());
        table.setTableStatus(tableStatusService.findByName(TableStatus.TableStatusName.AVAILABLE));
        RestaurantTable saved = restaurantTableRepository.save(table);
        seatService.generateSeatsForTable(saved, saved.getCapacity());
        return RestaurantTableResponse.from(saved);
    }

    public List<RestaurantTableResponse> findAll() {
        return restaurantTableRepository.findAll().stream()
                .map(RestaurantTableResponse::from)
                .toList();
    }

    public RestaurantTableResponse findById(UUID id) {
        return RestaurantTableResponse.from(getTable(id));
    }

    // Valida AVAILABLE dentro del service porque la entidad no está cargada en el punto de @PreAuthorize
    @Transactional
    public RestaurantTableResponse update(UUID id, RestaurantTableRequest request) {
        RestaurantTable table = getTable(id);
        requireAvailable(table);
        RestaurantTableResponse response = updateCapacityAndNumber(table, request.number(), request.capacity());
        return response;
    }

    @Transactional
    public RestaurantTableResponse updateCapacityAndNumber(RestaurantTable table, Integer number, Integer capacity) {
        table.setNumber(number);
        if (capacity > table.getCapacity()) {
            seatService.generateSeatsForTable(table, capacity);
        } else if (capacity < table.getCapacity()) {
            seatService.removeSeatsBeyondCapacity(table, capacity);
        }
        table.setCapacity(capacity);
        return RestaurantTableResponse.from(restaurantTableRepository.save(table));
    }

    @Transactional
    public void delete(UUID id) {
        RestaurantTable table = getTable(id);
        requireAvailable(table);
        restaurantTableRepository.delete(table);
    }

    // Actualiza el estado de una mesa y persiste el histórico con el usuario autenticado
    @Transactional
    public RestaurantTableResponse updateStatus(UUID id, UpdateTableStatusRequest request, User changedBy) {
        RestaurantTable table = getTable(id);
        TableStatus status = tableStatusService.findById(request.statusId());
        return tableStatusUpdateService.updateStatus(table, status, changedBy);
    }

    public List<TableStatusHistoryResponse> findHistory(UUID id) {
        getTable(id);
        return historyRepository.findByTableIdOrderByChangedAtDesc(id).stream()
                .map(TableStatusHistoryResponse::from)
                .toList();
    }

    private RestaurantTable getTable(UUID id) {
        return restaurantTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + id));
    }

    private void requireAvailable(RestaurantTable table) {
        if (table.getTableStatus().getName() != TableStatus.TableStatusName.AVAILABLE) {
            throw new BusinessRuleViolationException(
                    "La mesa " + table.getNumber() + " no está disponible para esta operación");
        }
    }
}
