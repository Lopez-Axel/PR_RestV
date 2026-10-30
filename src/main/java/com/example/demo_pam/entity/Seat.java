package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "seat", uniqueConstraints = {
        // Índice único para evitar números de silla duplicados dentro de una misma mesa
        @UniqueConstraint(name = "uk_seat_table_number", columnNames = {"table_id", "number"})
})
@Getter
@Setter
public class Seat extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false)
    private Boolean occupied = false;
}
