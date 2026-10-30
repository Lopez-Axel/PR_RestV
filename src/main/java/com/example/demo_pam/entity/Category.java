package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "category")
@Getter
@Setter
// Restringe todas las consultas a filas no borradas (borrado lógico unificado)
@SQLRestriction("deleted = false")
public class Category extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false NOT NULL")
    private Boolean deleted = false;
}