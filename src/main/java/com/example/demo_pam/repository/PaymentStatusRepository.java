package com.example.demo_pam.repository;

import com.example.demo_pam.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, UUID> {

    Optional<PaymentStatus> findByName(PaymentStatus.PaymentStatusName name);

    boolean existsByName(PaymentStatus.PaymentStatusName name);

    boolean existsByNameAndIdNot(PaymentStatus.PaymentStatusName name, UUID id);
}