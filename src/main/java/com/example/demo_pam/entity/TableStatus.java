package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "table_status")
@Getter
@Setter
public class TableStatus extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TableStatusName name;

    public enum TableStatusName {
        AVAILABLE, OCCUPIED, RESERVED, CLEANING
    }
}
