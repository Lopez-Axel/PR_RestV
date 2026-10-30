package com.example.demo_pam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu")
@Getter
@Setter
// Restringe todas las consultas a filas no borradas (borrado lógico unificado)
@SQLRestriction("deleted = false")
public class Menu extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false NOT NULL")
    private Boolean deleted = false;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenuDish> menuDishes = new ArrayList<>();
}