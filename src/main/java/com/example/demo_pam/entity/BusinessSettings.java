package com.example.demo_pam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "business_settings")
@Getter
@Setter
public class BusinessSettings {

    @Id
    private Long id = 1L;

    @Column(name = "default_margin_percentage", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultMarginPercentage = new BigDecimal("30.00");

    @Column(name = "min_margin_percentage", nullable = false, precision = 10, scale = 2)
    private BigDecimal minMarginPercentage = new BigDecimal("10.00");

    @Column(name = "max_margin_percentage", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxMarginPercentage = new BigDecimal("60.00");

    // Contador de folios de recibo: se incrementa de forma atómica al confirmar cada pago
    @Column(name = "receipt_counter", nullable = false)
    private Long receiptCounter = 0L;
}