package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.OrderStatusRequest;
import com.example.demo_pam.dto.response.OrderStatusResponse;
import com.example.demo_pam.entity.OrderStatus;
import com.example.demo_pam.exception.InvalidOrderException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.OrderRepository;
import com.example.demo_pam.repository.OrderStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderStatusService {

    private final OrderStatusRepository orderStatusRepository;
    private final OrderRepository orderRepository;

    public OrderStatusService(OrderStatusRepository orderStatusRepository, OrderRepository orderRepository) {
        this.orderStatusRepository = orderStatusRepository;
        this.orderRepository = orderRepository;
    }

    public List<OrderStatusResponse> findAll() {
        return orderStatusRepository.findAll().stream().map(OrderStatusResponse::from).toList();
    }

    public OrderStatusResponse findById(UUID id) {
        return OrderStatusResponse.from(getById(id));
    }

    public OrderStatus findByName(OrderStatus.OrderStatusName name) {
        return orderStatusRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pedido no encontrado: " + name));
    }

    @Transactional
    public OrderStatusResponse create(OrderStatusRequest request) {
        if (orderStatusRepository.existsByName(request.name())) {
            throw new InvalidOrderException("Ya existe un estado con el nombre " + request.name());
        }
        OrderStatus status = new OrderStatus();
        status.setName(request.name());
        status.setColorHex(request.colorHex());
        return OrderStatusResponse.from(orderStatusRepository.save(status));
    }

    // Permite cambiar el color siempre; el nombre solo si no entró en uso (evita romper el flujo del enum)
    @Transactional
    public OrderStatusResponse update(UUID id, OrderStatusRequest request) {
        OrderStatus status = getById(id);
        if (orderStatusRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new InvalidOrderException("Ya existe un estado con el nombre " + request.name());
        }
        if (status.getName() != request.name() && orderRepository.countByOrderStatusId(id) > 0) {
            throw new InvalidOrderException("No se puede renombrar un estado referenciado por pedidos");
        }
        status.setName(request.name());
        status.setColorHex(request.colorHex());
        return OrderStatusResponse.from(status);
    }

    // Se borran solo estados que nunca entraron en uso; los usados se protegen para preservar el histórico
    @Transactional
    public void delete(UUID id) {
        OrderStatus status = getById(id);
        if (orderRepository.countByOrderStatusId(id) > 0) {
            throw new InvalidOrderException("No se puede eliminar un estado referenciado por pedidos");
        }
        orderStatusRepository.delete(status);
    }

    private OrderStatus getById(UUID id) {
        return orderStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pedido no encontrado: " + id));
    }
}