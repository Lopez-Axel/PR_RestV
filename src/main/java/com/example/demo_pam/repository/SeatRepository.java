package com.example.demo_pam.repository;

import com.example.demo_pam.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByTableId(UUID tableId);
}
