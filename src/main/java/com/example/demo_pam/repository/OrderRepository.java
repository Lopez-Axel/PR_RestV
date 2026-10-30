package com.example.demo_pam.repository;

import com.example.demo_pam.entity.Order;
import com.example.demo_pam.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Regla de negocio: una misma mesa no puede tener dos pedidos abiertos a la vez
    boolean existsByTableIdAndOrderStatus_NameNotIn(UUID tableId, Collection<OrderStatus.OrderStatusName> names);

    // FETCH JOIN de las asociaciones que OrderResponse.from toca, para evitar N+1 al listar
    @EntityGraph(attributePaths = {"user", "table", "orderStatus", "orderDetails"})
    Page<Order> findByTableIdOrderByDateTimeDesc(UUID tableId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "table", "orderStatus", "orderDetails"})
    Page<Order> findByUserIdOrderByDateTimeDesc(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "table", "orderStatus", "orderDetails"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "table", "orderStatus", "orderDetails"})
    Optional<Order> findById(UUID uuid);

    long countByOrderStatusId(UUID orderStatusId);
}