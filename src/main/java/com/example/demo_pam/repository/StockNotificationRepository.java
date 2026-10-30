package com.example.demo_pam.repository;

import com.example.demo_pam.entity.StockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockNotificationRepository extends JpaRepository<StockNotification, UUID> {

    List<StockNotification> findByStatusAndNextAttemptAtLessThanEqual(StockNotification.Status status, Instant threshold);

    List<StockNotification> findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            StockNotification.Status status, int maxAttempts, Instant threshold);

    boolean existsByStatusAndNextAttemptAtGreaterThan(StockNotification.Status status, Instant threshold);

    boolean existsByStatusAndAttemptCountLessThan(StockNotification.Status status, int maxAttempts);

    List<StockNotification> findTop10ByOrderByCreatedAtDesc();
}
