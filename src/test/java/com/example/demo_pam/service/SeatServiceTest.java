package com.example.demo_pam.service;

import com.example.demo_pam.entity.RestaurantTable;
import com.example.demo_pam.entity.Seat;
import com.example.demo_pam.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    @Test
    void generateSeatsForTable_createsSeatsFromOneToCapacity() {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        when(seatRepository.findByTableId(table.getId())).thenReturn(List.of());

        seatService.generateSeatsForTable(table, 4);

        ArgumentCaptor<Seat> captor = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepository, times(4)).save(captor.capture());
        List<Integer> numbers = captor.getAllValues().stream()
                .map(Seat::getNumber)
                .sorted()
                .toList();
        assertThat(numbers).containsExactly(1, 2, 3, 4);
        captor.getAllValues().forEach(seat -> assertThat(seat.getOccupied()).isFalse());
    }

    @Test
    void generateSeatsForTable_continuesFromLastNumberWhenSeatsExist() {
        RestaurantTable table = new RestaurantTable();
        table.setId(UUID.randomUUID());
        Seat existing = new Seat();
        existing.setNumber(3);
        when(seatRepository.findByTableId(table.getId())).thenReturn(List.of(existing));

        seatService.generateSeatsForTable(table, 5);

        ArgumentCaptor<Seat> captor = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepository, times(2)).save(captor.capture());
        List<Integer> numbers = captor.getAllValues().stream()
                .map(Seat::getNumber)
                .sorted()
                .toList();
        assertThat(numbers).containsExactly(4, 5);
    }
}
