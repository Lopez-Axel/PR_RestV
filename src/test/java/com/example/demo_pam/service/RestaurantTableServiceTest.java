package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.RestaurantTableRequest;
import com.example.demo_pam.entity.RestaurantTable;
import com.example.demo_pam.entity.TableStatus;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.repository.RestaurantTableRepository;
import com.example.demo_pam.repository.TableStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;
    @Mock
    private TableStatusHistoryRepository historyRepository;
    @Mock
    private TableStatusService tableStatusService;
    @Mock
    private SeatService seatService;
    @Mock
    private TableStatusUpdateService tableStatusUpdateService;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

    private TableStatus available;
    private TableStatus occupied;

    @BeforeEach
    void setUp() {
        available = new TableStatus();
        available.setName(TableStatus.TableStatusName.AVAILABLE);
        occupied = new TableStatus();
        occupied.setName(TableStatus.TableStatusName.OCCUPIED);
    }

    @Test
    void update_throwsWhenTableIsNotAvailable() {
        RestaurantTable table = tableWithStatus(occupied);
        when(restaurantTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() ->
                restaurantTableService.update(table.getId(), new RestaurantTableRequest(1, 4)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void update_doesNothingElseWhenTableIsNotAvailable() {
        RestaurantTable table = tableWithStatus(occupied);
        when(restaurantTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() ->
                restaurantTableService.update(table.getId(), new RestaurantTableRequest(1, 4)))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(seatService, never()).generateSeatsForTable(null, 4);
        verify(seatService, never()).removeSeatsBeyondCapacity(null, 4);
    }

    @Test
    void delete_throwsWhenTableIsNotAvailable() {
        RestaurantTable table = tableWithStatus(occupied);
        when(restaurantTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> restaurantTableService.delete(table.getId()))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(restaurantTableRepository, never()).delete(table);
    }

    @Test
    void delete_allowsWhenTableIsAvailable() {
        RestaurantTable table = tableWithStatus(available);
        when(restaurantTableRepository.findById(table.getId())).thenReturn(Optional.of(table));

        restaurantTableService.delete(table.getId());

        verify(restaurantTableRepository).delete(table);
    }

    private RestaurantTable tableWithStatus(TableStatus status) {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        table.setNumber(1);
        table.setCapacity(4);
        table.setTableStatus(status);
        return table;
    }
}
