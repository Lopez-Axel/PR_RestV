package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.RestaurantTableResponse;
import com.example.demo_pam.entity.RestaurantTable;
import com.example.demo_pam.entity.TableStatus;
import com.example.demo_pam.entity.TableStatusHistory;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.repository.RestaurantTableRepository;
import com.example.demo_pam.repository.TableStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TableStatusUpdateService {

    private final RestaurantTableRepository restaurantTableRepository;
    private final TableStatusHistoryRepository historyRepository;
    private final TableStatusSseService sseService;

    public TableStatusUpdateService(RestaurantTableRepository restaurantTableRepository,
                                    TableStatusHistoryRepository historyRepository,
                                    TableStatusSseService sseService) {
        this.restaurantTableRepository = restaurantTableRepository;
        this.historyRepository = historyRepository;
        this.sseService = sseService;
    }

    // Actualiza el estado actual, registra el histórico y notifica a los clientes SSE en una sola transacción
    @Transactional
    public RestaurantTableResponse updateStatus(RestaurantTable table, TableStatus newStatus, User changedBy) {
        table.setTableStatus(newStatus);
        restaurantTableRepository.save(table);

        TableStatusHistory history = new TableStatusHistory();
        history.setTable(table);
        history.setStatus(newStatus);
        history.setChangedAt(Instant.now());
        history.setChangedBy(changedBy);
        historyRepository.save(history);

        RestaurantTableResponse response = RestaurantTableResponse.from(table);
        sseService.broadcast(response);
        return response;
    }
}
