package com.example.demo_pam.repository;

import com.example.demo_pam.entity.Payment;
import com.example.demo_pam.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    Page<Payment> findByPaymentStatusName(PaymentStatus.PaymentStatusName name, Pageable pageable);

    Optional<Payment> findByBankTransactionId(String bankTransactionId);

    // Suma cobrada de una orden en cierto estado; derivado no alcanza para un agregado
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.order.id = :orderId and p.paymentStatus.name = :status and p.deleted = false")
    BigDecimal sumAmountByOrderIdAndPaymentStatusName(@Param("orderId") UUID orderId,
                                                      @Param("status") PaymentStatus.PaymentStatusName status);

    long countByPaymentStatusId(UUID paymentStatusId);
}