package com.example.demo_pam.repository;

import com.example.demo_pam.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, UUID> {

    Optional<OrderStatus> findByName(OrderStatus.OrderStatusName name);

    boolean existsByName(OrderStatus.OrderStatusName name);

    boolean existsByNameAndIdNot(OrderStatus.OrderStatusName name, UUID id);
}