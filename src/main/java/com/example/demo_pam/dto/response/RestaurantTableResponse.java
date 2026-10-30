package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.RestaurantTable;

import java.util.UUID;

public record RestaurantTableResponse(
        UUID id,
        Integer number,
        Integer capacity,
        String status
) {

    public static RestaurantTableResponse from(RestaurantTable table) {
        return new RestaurantTableResponse(
                table.getId(),
                table.getNumber(),
                table.getCapacity(),
                table.getTableStatus().getName().name()
        );
    }
}
