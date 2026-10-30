package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.PaymentStatusRequest;
import com.example.demo_pam.dto.response.PaymentStatusResponse;
import com.example.demo_pam.entity.PaymentStatus;
import com.example.demo_pam.exception.InvalidOrderException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.PaymentRepository;
import com.example.demo_pam.repository.PaymentStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentStatusService {

    private final PaymentStatusRepository paymentStatusRepository;
    private final PaymentRepository paymentRepository;

    public PaymentStatusService(PaymentStatusRepository paymentStatusRepository, PaymentRepository paymentRepository) {
        this.paymentStatusRepository = paymentStatusRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentStatusResponse> findAll() {
        return paymentStatusRepository.findAll().stream().map(PaymentStatusResponse::from).toList();
    }

    public PaymentStatusResponse findById(UUID id) {
        return PaymentStatusResponse.from(getById(id));
    }

    public PaymentStatus findByName(PaymentStatus.PaymentStatusName name) {
        return paymentStatusRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pago no encontrado: " + name));
    }

    @Transactional
    public PaymentStatusResponse create(PaymentStatusRequest request) {
        if (paymentStatusRepository.existsByName(request.name())) {
            throw new InvalidOrderException("Ya existe un estado con el nombre " + request.name());
        }
        PaymentStatus status = new PaymentStatus();
        status.setName(request.name());
        return PaymentStatusResponse.from(paymentStatusRepository.save(status));
    }

    // El nombre solo cambia si el estado aún no fue usado por ningún pago
    @Transactional
    public PaymentStatusResponse update(UUID id, PaymentStatusRequest request) {
        PaymentStatus status = getById(id);
        if (paymentStatusRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new InvalidOrderException("Ya existe un estado con el nombre " + request.name());
        }
        if (status.getName() != request.name() && paymentRepository.countByPaymentStatusId(id) > 0) {
            throw new InvalidOrderException("No se puede renombrar un estado referenciado por pagos");
        }
        status.setName(request.name());
        return PaymentStatusResponse.from(status);
    }

    // Se borran solo estados que nunca entraron en uso; los usados se protegen para preservar el histórico
    @Transactional
    public void delete(UUID id) {
        PaymentStatus status = getById(id);
        if (paymentRepository.countByPaymentStatusId(id) > 0) {
            throw new InvalidOrderException("No se puede eliminar un estado referenciado por pagos");
        }
        paymentStatusRepository.delete(status);
    }

    private PaymentStatus getById(UUID id) {
        return paymentStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado de pago no encontrado: " + id));
    }
}