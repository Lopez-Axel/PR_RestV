package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Seat;

import java.util.UUID;

public record SeatResponse(
        UUID id,
        Integer number,
        Boolean occupied
) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getNumber(), seat.getOccupied());
    }
}
