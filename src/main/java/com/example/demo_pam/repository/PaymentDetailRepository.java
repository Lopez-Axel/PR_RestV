package com.example.demo_pam.repository;

import com.example.demo_pam.entity.PaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, UUID> {

    List<PaymentDetail> findByPaymentId(UUID paymentId);

    List<PaymentDetail> findByPayment_Order_Id(UUID orderId);

    List<PaymentDetail> findByOrderDetailId(UUID orderDetailId);
}