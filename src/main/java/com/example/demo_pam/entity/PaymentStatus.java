package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "payment_status")
@Getter
@Setter
// Restringe todas las consultas a filas no borradas (borrado lógico unificado)
@SQLRestriction("deleted = false")
public class PaymentStatus extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PaymentStatusName name;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false NOT NULL")
    private Boolean deleted = false;

    public enum PaymentStatusName {
        PENDING, CONFIRMED, FAILED, EXPIRED
    }
}