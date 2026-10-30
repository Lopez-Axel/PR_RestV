package com.example.demo_pam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_alert")
@Getter
@Setter
public class StockAlert extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false, unique = true)
    private Ingredient ingredient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "notified_current_stock", precision = 12, scale = 2)
    private BigDecimal notifiedCurrentStock;

    @Column(name = "notified_minimum_stock", precision = 12, scale = 2)
    private BigDecimal notifiedMinimumStock;

    public enum Status {
        OPEN, NOTIFIED, RESOLVED
    }
}
