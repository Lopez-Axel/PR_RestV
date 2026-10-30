package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.OrderCreateRequest;
import com.example.demo_pam.dto.request.OrderDetailCreateRequest;
import com.example.demo_pam.dto.request.OrderStatusUpdateRequest;
import com.example.demo_pam.dto.response.OrderDetailResponse;
import com.example.demo_pam.dto.response.OrderResponse;
import com.example.demo_pam.entity.*;
import com.example.demo_pam.exception.InvalidOrderException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.OrderRepository;
import com.example.demo_pam.repository.RestaurantTableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final List<OrderStatus.OrderStatusName> OPEN_EXCLUDED =
            List.of(OrderStatus.OrderStatusName.CLOSED, OrderStatus.OrderStatusName.CANCELLED);

    private final OrderRepository orderRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final MenuDishService menuDishService;
    private final OrderStatusService orderStatusService;
    private final TableStatusUpdateService tableStatusUpdateService;
    private final TableStatusService tableStatusService;
    private final OrderStatusSseService sseService;

    public OrderService(OrderRepository orderRepository,
                        RestaurantTableRepository restaurantTableRepository,
                        MenuDishService menuDishService,
                        OrderStatusService orderStatusService,
                        TableStatusUpdateService tableStatusUpdateService,
                        TableStatusService tableStatusService,
                        OrderStatusSseService sseService) {
        this.orderRepository = orderRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.menuDishService = menuDishService;
        this.orderStatusService = orderStatusService;
        this.tableStatusUpdateService = tableStatusUpdateService;
        this.tableStatusService = tableStatusService;
        this.sseService = sseService;
    }

    // Crea el pedido en una sola transacción: valida mesa, platos en menú activo y marca la mesa OCCUPIED
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, User user) {
        RestaurantTable table = getTable(request.tableId());
        if (orderRepository.existsByTableIdAndOrderStatus_NameNotIn(table.getId(), OPEN_EXCLUDED)) {
            throw new InvalidOrderException("La mesa " + table.getNumber() +
                    " ya tiene un pedido abierto: agregue platos al pedido actual");
        }

        Order order = new Order();
        order.setTable(table);
        order.setUser(user);
        order.setOrderStatus(orderStatusService.findByName(OrderStatus.OrderStatusName.PENDING));
        order.setDateTime(LocalDateTime.now());
        order.setNotes(request.notes());
        request.details().forEach(dto -> order.getOrderDetails().add(buildDetail(order, dto)));

        Order saved = orderRepository.save(order);
        tableStatusUpdateService.updateStatus(table,
                tableStatusService.findByName(TableStatus.TableStatusName.OCCUPIED), user);
        OrderResponse response = OrderResponse.from(saved);
        sseService.broadcast(response);
        return response;
    }

    // Agrega un plato a un pedido existente y abierto
    @Transactional
    public OrderDetailResponse addDetail(UUID orderId, OrderDetailCreateRequest dto, User user) {
        Order order = getOrder(orderId);
        requireOpen(order);
        OrderDetail detail = buildDetail(order, dto);
        order.getOrderDetails().add(detail);
        Order saved = orderRepository.save(order);
        sseService.broadcast(OrderResponse.from(saved));
        return OrderDetailResponse.from(detail);
    }

    // Cambia el estado; CLOSED solo puede originarse desde el pago
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatusUpdateRequest request, User user) {
        Order order = getOrder(orderId);
        requireOpen(order);
        if (request.statusName() == OrderStatus.OrderStatusName.CLOSED) {
            throw new InvalidOrderException("El estado CLOSED solo se confirma desde el pago de la orden");
        }
        order.setOrderStatus(orderStatusService.findByName(request.statusName()));
        Order saved = orderRepository.save(order);
        OrderResponse response = OrderResponse.from(saved);
        sseService.broadcast(response);
        return response;
    }

    @Transactional
    public OrderResponse cancel(UUID orderId, User user) {
        Order order = getOrder(orderId);
        requireOpen(order);
        order.setOrderStatus(orderStatusService.findByName(OrderStatus.OrderStatusName.CANCELLED));
        Order saved = orderRepository.save(order);
        OrderResponse response = OrderResponse.from(saved);
        sseService.broadcast(response);
        return response;
    }

    // Borrado lógico por ADMIN: el pedido deja de listarse pero conserva su fila
    @Transactional
    public void softDelete(UUID orderId) {
        Order order = getOrder(orderId);
        order.setDeleted(true);
        orderRepository.save(order);
    }

    // Lecturas con transacción de solo lectura: el mapeo a OrderResponse toca asociaciones lazy
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID orderId) {
        return OrderResponse.from(getOrder(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listByTable(UUID tableId, Pageable pageable) {
        getTable(tableId);
        return orderRepository.findByTableIdOrderByDateTimeDesc(tableId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listByUser(UUID userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByDateTimeDesc(userId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> listDetails(UUID orderId) {
        Order order = getOrder(orderId);
        return order.getOrderDetails().stream().map(OrderDetailResponse::from).toList();
    }

    // Calcula el subtotal y copia el precio como snapshot para que no cambie con futuras subidas
    private OrderDetail buildDetail(Order order, OrderDetailCreateRequest dto) {
        MenuDish menuDish = menuDishService.findDishInActiveMenu(dto.dishId());
        Dish dish = menuDish.getDish();
        if (dish.getSellingPrice() == null) {
            throw new InvalidOrderException("El plato " + dish.getName() + " no tiene precio de venta");
        }
        BigDecimal subtotal = dish.getSellingPrice()
                .multiply(BigDecimal.valueOf(dto.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setDish(dish);
        detail.setQuantity(dto.quantity());
        detail.setUnitPrice(dish.getSellingPrice());
        detail.setSubtotal(subtotal);
        return detail;
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));
    }

    private RestaurantTable getTable(UUID tableId) {
        return restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada: " + tableId));
    }

    private void requireOpen(Order order) {
        OrderStatus.OrderStatusName name = order.getOrderStatus().getName();
        if (name == OrderStatus.OrderStatusName.CLOSED || name == OrderStatus.OrderStatusName.CANCELLED) {
            throw new InvalidOrderException("El pedido está " + name + " y no acepta cambios");
        }
    }
}