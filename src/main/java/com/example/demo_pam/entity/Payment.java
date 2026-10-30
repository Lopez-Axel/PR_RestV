package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Getter
@Setter
// Restringe todas las consultas a filas no borradas (borrado lógico unificado)
@SQLRestriction("deleted = false")
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Método de pago: efectivo, transferencia con QR dinámico del banco o QR estático del local
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, columnDefinition = "varchar(255) NOT NULL DEFAULT 'QR'")
    private PaymentMethod paymentMethod = PaymentMethod.QR;

    // QR generado y codificado como PNG en Base64; null en efectivo (CASH)
    @Column(name = "qr_code", columnDefinition = "text")
    private String qrCode;

    // Referencia devuelta por la API del banco para correlacionar el callback (solo TRANSFER)
    @Column(name = "bank_transaction_id")
    private String bankTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_id", nullable = false)
    private PaymentStatus paymentStatus;

    // Nombre de la persona que paga esta parte; se carga al dividir o al confirmar manualmente
    @Column(name = "payee_name")
    private String payeeName;

    // Líneas de la orden cubiertas por este pago; la suma de sus subtotales es el amount de la parte
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentDetail> paymentDetails = new ArrayList<>();

    // Folio del recibo, se asigna una sola vez al confirmar el pago
    @Column(name = "receipt_number", unique = true)
    private Long receiptNumber;

    @Column(name = "bank_reference")
    private String bankReference;

    @Column(name = "payment_date")
    private Instant paymentDate;

    // El QR deja de ser válido al vencer; se resuelve de forma perezosa al leer
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Optimistic lock: protege la doble confirmación (callback del banco + confirm manual simultáneos)
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false NOT NULL")
    private Boolean deleted = false;

    public enum PaymentMethod {
        CASH, TRANSFER, QR
    }
}