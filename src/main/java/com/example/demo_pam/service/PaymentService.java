package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.PaymentCreateRequest;
import com.example.demo_pam.dto.request.SplitPaymentRequest;
import com.example.demo_pam.dto.response.OrderResponse;
import com.example.demo_pam.dto.response.PaymentReceiptResponse;
import com.example.demo_pam.dto.response.PaymentResponse;
import com.example.demo_pam.entity.*;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.InvalidOrderException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.BusinessSettingsRepository;
import com.example.demo_pam.repository.OrderDetailRepository;
import com.example.demo_pam.repository.OrderRepository;
import com.example.demo_pam.repository.PaymentDetailRepository;
import com.example.demo_pam.repository.PaymentRepository;
import com.example.demo_pam.repository.PaymentStatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Set<PaymentStatus.PaymentStatusName> LOCKING_STATUSES =
            Set.of(PaymentStatus.PaymentStatusName.PENDING, PaymentStatus.PaymentStatusName.CONFIRMED);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final OrderStatusService orderStatusService;
    private final TableStatusUpdateService tableStatusUpdateService;
    private final TableStatusService tableStatusService;
    private final OrderStatusSseService orderStatusSseService;
    private final BankPaymentProvider bankPaymentProvider;
    private final String staticQrImagePath;
    private final int expirationMinutes;

    private byte[] staticQrImageCache;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          OrderDetailRepository orderDetailRepository,
                          PaymentDetailRepository paymentDetailRepository,
                          PaymentStatusRepository paymentStatusRepository,
                          BusinessSettingsRepository businessSettingsRepository,
                          OrderStatusService orderStatusService,
                          TableStatusUpdateService tableStatusUpdateService,
                          TableStatusService tableStatusService,
                          OrderStatusSseService orderStatusSseService,
                          BankPaymentProvider bankPaymentProvider,
                          @Value("${app.payment.static-qr-image}") String staticQrImagePath,
                          @Value("${app.payment.qr-expiration-minutes:10}") int expirationMinutes) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.businessSettingsRepository = businessSettingsRepository;
        this.orderStatusService = orderStatusService;
        this.tableStatusUpdateService = tableStatusUpdateService;
        this.tableStatusService = tableStatusService;
        this.orderStatusSseService = orderStatusSseService;
        this.bankPaymentProvider = bankPaymentProvider;
        this.staticQrImagePath = staticQrImagePath;
        this.expirationMinutes = expirationMinutes;
    }

    // Crea la parte "el resto": cubre todas las líneas aún no cobradas de la orden
    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request, User user) {
        Order order = findServedOrder(request.orderId());
        List<OrderDetail> uncovered = uncoveredLines(order.getId());
        if (uncovered.isEmpty()) {
            throw new BusinessRuleViolationException("El pedido ya fue pagado");
        }
        BigDecimal amount = sum(uncovered);
        Payment payment = buildPayment(order, amount, request.paymentMethod(), null, uncovered);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    // Divide la cuenta en una parte por persona; cada parte recibe líneas disjuntas y su propio QR
    @Transactional
    public List<PaymentResponse> createSplit(UUID orderId, SplitPaymentRequest request, User user) {
        Order order = findServedOrder(orderId);
        List<OrderDetail> uncovered = uncoveredLines(order.getId());
        if (uncovered.isEmpty()) {
            throw new BusinessRuleViolationException("El pedido ya fue pagado");
        }
        Set<UUID> uncoveredIds = uncovered.stream().map(OrderDetail::getId).collect(Collectors.toSet());
        Set<UUID> assignedIds = new HashSet<>();
        List<Payment> payments = new java.util.ArrayList<>();

        for (SplitPaymentRequest.Part part : request.parts()) {
            List<OrderDetail> lines = part.orderDetailIds().stream()
                    .map(id -> findDetailInOrder(order, id, uncoveredIds, assignedIds))
                    .toList();
            assignedIds.addAll(lines.stream().map(OrderDetail::getId).toList());
            payments.add(buildPayment(order, sum(lines), part.paymentMethod(), part.personName(), lines));
        }
        return paymentRepository.saveAll(payments).stream().map(PaymentResponse::from).toList();
    }

    @Transactional
    public Page<PaymentResponse> findAll(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(PaymentResponse::from);
    }

    @Transactional
    public Page<PaymentResponse> listByStatus(PaymentStatus.PaymentStatusName status, Pageable pageable) {
        return paymentRepository.findByPaymentStatusName(status, pageable).map(PaymentResponse::from);
    }

    // Confirmación manual: WAITER/ADMIN para CASH y QR; la transferencia se confirma sola por el banco
    // (el fallback manual de TRANSFER queda reservado al ADMIN)
    @Transactional
    public PaymentResponse confirmPayment(UUID paymentId, String payeeName, String bankReference, User user) {
        Payment payment = getPayment(paymentId);
        if (payment.getPaymentMethod() == Payment.PaymentMethod.TRANSFER && user.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Solo el ADMIN confirma transferencias manualmente");
        }
        expireIfNeeded(payment);
        if (payment.getPaymentStatus().getName() == PaymentStatus.PaymentStatusName.EXPIRED) {
            throw new InvalidOrderException("El intento de pago venció, generá uno nuevo");
        }
        if (payment.getPaymentStatus().getName() != PaymentStatus.PaymentStatusName.PENDING) {
            throw new InvalidOrderException("El pago ya fue procesado");
        }
        if (payeeName != null && !payeeName.isBlank() && payment.getPayeeName() == null) {
            payment.setPayeeName(payeeName);
        }
        return settle(payment, bankReference, user);
    }

    // Asienta una parte: asigna folio y, solo cuando la suma cobrada cubre el total, cierra la orden y libera la mesa
    @Transactional
    public PaymentResponse settle(Payment payment, String bankReference, User changedBy) {
        payment.setPaymentStatus(paymentStatusRepository
                .findByName(PaymentStatus.PaymentStatusName.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pago CONFIRMED no encontrado")));
        if (bankReference != null && !bankReference.isBlank()) {
            payment.setBankReference(bankReference);
        } else if (payment.getPaymentMethod() == Payment.PaymentMethod.CASH) {
            payment.setBankReference("CASH-" + UUID.randomUUID());
        }
        payment.setPaymentDate(Instant.now());
        if (payment.getReceiptNumber() == null) {
            payment.setReceiptNumber(nextReceiptNumber());
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        BigDecimal orderTotal = sum(orderDetailRepository.findByOrderId(order.getId()));
        BigDecimal confirmed = paymentRepository.sumAmountByOrderIdAndPaymentStatusName(
                order.getId(), PaymentStatus.PaymentStatusName.CONFIRMED);
        if (confirmed.compareTo(orderTotal) >= 0 && orderTotal.compareTo(BigDecimal.ZERO) > 0) {
            order.setOrderStatus(orderStatusService.findByName(OrderStatus.OrderStatusName.CLOSED));
            Order saved = orderRepository.save(order);
            orderStatusSseService.broadcast(OrderResponse.from(saved));
            tableStatusUpdateService.updateStatus(order.getTable(),
                    tableStatusService.findByName(TableStatus.TableStatusName.AVAILABLE), changedBy);
        }
        return PaymentResponse.from(payment);
    }

    // Callback del banco: correlaciona por bankTransactionId y asienta o falla el pago (idempotente)
    @Transactional
    public void settleByBankTransaction(String transactionId, String outcome, String bankReference) {
        Payment payment = paymentRepository.findByBankTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un pago con la referencia " + transactionId));
        if (payment.getPaymentStatus().getName() != PaymentStatus.PaymentStatusName.PENDING) {
            return;
        }
        expireIfNeeded(payment);
        if (payment.getPaymentStatus().getName() != PaymentStatus.PaymentStatusName.PENDING) {
            return;
        }
        if (outcome.equalsIgnoreCase("SUCCESS")) {
            settle(payment, bankReference, null);
        } else {
            markFailed(payment);
        }
    }

    // Marca explícitamente un intento de pago como fallido (libera sus líneas para re-emitir la parte)
    @Transactional
    public PaymentResponse failPayment(UUID paymentId, User user) {
        Payment payment = getPayment(paymentId);
        expireIfNeeded(payment);
        if (payment.getPaymentStatus().getName() == PaymentStatus.PaymentStatusName.EXPIRED) {
            throw new InvalidOrderException("El intento de pago venció, generá uno nuevo");
        }
        if (payment.getPaymentStatus().getName() != PaymentStatus.PaymentStatusName.PENDING) {
            throw new InvalidOrderException("Solo se puede marcar como fallido un pago PENDING");
        }
        return markFailed(payment);
    }

    // Soft delete: un pago confirmado es evidencia contable y no se puede quitar del historial
    @Transactional
    public void softDelete(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        if (payment.getPaymentStatus().getName() == PaymentStatus.PaymentStatusName.CONFIRMED) {
            throw new InvalidOrderException("No se puede eliminar un pago confirmado");
        }
        payment.setDeleted(true);
        paymentRepository.save(payment);
    }

    // Recibo imprimible: solo los pagos confirmados tienen folio asignado
    @Transactional
    public PaymentReceiptResponse getReceipt(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        expireIfNeeded(payment);
        if (payment.getPaymentStatus().getName() != PaymentStatus.PaymentStatusName.CONFIRMED) {
            throw new BusinessRuleViolationException("El recibo solo está disponible para pagos confirmados");
        }
        return PaymentReceiptResponse.from(payment);
    }

    @Transactional
    public PaymentResponse findById(UUID paymentId) {
        Payment payment = getPayment(paymentId);
        expireIfNeeded(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public List<PaymentResponse> listByOrder(UUID orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        payments.forEach(this::expireIfNeeded);
        return payments.stream().map(PaymentResponse::from).toList();
    }

    private Order findServedOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));
        if (order.getOrderStatus().getName() != OrderStatus.OrderStatusName.SERVED) {
            throw new InvalidOrderException("El pedido debe estar servido antes de cobrar");
        }
        return order;
    }

    // Líneas libres = las que ningún pago PENDING/CONFIRMED está cubriendo (FAILED/EXPIRED las liberan)
    private List<OrderDetail> uncoveredLines(UUID orderId) {
        List<OrderDetail> all = orderDetailRepository.findByOrderId(orderId);
        Set<UUID> locked = paymentDetailRepository.findByPayment_Order_Id(orderId).stream()
                .filter(pd -> pd.getPayment().getPaymentStatus() != null
                        && LOCKING_STATUSES.contains(pd.getPayment().getPaymentStatus().getName()))
                .map(pd -> pd.getOrderDetail().getId())
                .collect(Collectors.toSet());
        return all.stream().filter(d -> !locked.contains(d.getId())).toList();
    }

    private OrderDetail findDetailInOrder(Order order, UUID detailId, Set<UUID> uncoveredIds, Set<UUID> assignedIds) {
        OrderDetail detail = order.getOrderDetails() == null ? null
                : order.getOrderDetails().stream().filter(d -> d.getId().equals(detailId)).findFirst().orElse(null);
        if (detail == null) {
            throw new InvalidOrderException("Una línea no pertenece al pedido " + order.getId());
        }
        if (!uncoveredIds.contains(detailId) || assignedIds.contains(detailId)) {
            throw new BusinessRuleViolationException("La línea " + detailId + " ya está cubierta por otro pago");
        }
        return detail;
    }

    private Payment buildPayment(Order order, BigDecimal amount, Payment.PaymentMethod method,
                                 String payeeName, List<OrderDetail> lines) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPayeeName(payeeName);
        payment.setPaymentStatus(paymentStatusRepository
                .findByName(PaymentStatus.PaymentStatusName.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pago PENDING no encontrado")));
        payment.setExpiresAt(Instant.now().plusSeconds(expirationMinutes * 60L));

        if (method == Payment.PaymentMethod.TRANSFER) {
            BankPaymentProvider.DynamicQr dynamicQr = bankPaymentProvider.createDynamicQr(UUID.randomUUID(), amount);
            payment.setQrCode(dynamicQr.imageBase64());
            payment.setBankTransactionId(dynamicQr.transactionId());
        } else if (method == Payment.PaymentMethod.QR) {
            payment.setQrCode(staticQrBase64());
        }

        for (OrderDetail line : lines) {
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setPayment(payment);
            paymentDetail.setOrderDetail(line);
            paymentDetail.setSubtotal(line.getSubtotal());
            payment.getPaymentDetails().add(paymentDetail);
        }
        return payment;
    }

    // Folio secuencial: lock pesimista sobre la configuración para no quemar dos folios iguales en paralelo
    private long nextReceiptNumber() {
        BusinessSettings settings = businessSettingsRepository.findByIdWithLock()
                .orElseGet(() -> {
                    BusinessSettings created = new BusinessSettings();
                    created.setId(1L);
                    return created;
                });
        settings.setReceiptCounter(settings.getReceiptCounter() + 1);
        businessSettingsRepository.save(settings);
        return settings.getReceiptCounter();
    }

    private BigDecimal sum(List<OrderDetail> details) {
        return details.stream()
                .map(OrderDetail::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + paymentId));
    }

    private PaymentResponse markFailed(Payment payment) {
        payment.setPaymentStatus(paymentStatusRepository
                .findByName(PaymentStatus.PaymentStatusName.FAILED)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pago FAILED no encontrado")));
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    // Expiración perezosa: se resuelve al leer, sin scheduler de background
    private void expireIfNeeded(Payment payment) {
        if (payment.getPaymentMethod() == Payment.PaymentMethod.CASH) {
            return;
        }
        if (payment.getPaymentStatus().getName() == PaymentStatus.PaymentStatusName.PENDING
                && Instant.now().isAfter(payment.getExpiresAt())) {
            payment.setPaymentStatus(paymentStatusRepository
                    .findByName(PaymentStatus.PaymentStatusName.EXPIRED)
                    .orElseThrow(() -> new ResourceNotFoundException("Estado de pago EXPIRED no encontrado")));
            paymentRepository.save(payment);
        }
    }

    // Imagen estática del QR del local, cacheada en memoria tras la primera lectura
    private String staticQrBase64() {
        if (staticQrImageCache == null) {
            try {
                staticQrImageCache = Files.readAllBytes(Path.of(staticQrImagePath));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer la imagen QR estática en " + staticQrImagePath, e);
            }
        }
        return Base64.getEncoder().encodeToString(staticQrImageCache);
    }
}