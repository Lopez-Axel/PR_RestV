package com.example.demo_pam.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
// AuditingEntityListener puebla automáticamente createdAt/updatedAt y sus autores en cada persist/update
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    // Anotación de auditoría: Spring Data asigna el timestamp al insertar sin código manual
    @CreatedDate
    private Instant createdAt;

    // Anotación de auditoría: Spring Data asigna el timestamp al actualizar sin código manual
    @LastModifiedDate
    private Instant updatedAt;

    // Anotación de auditoría: Spring Data asigna el usuario que creó la fila (desde AuditorAware)
    @CreatedBy
    @ManyToOne
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    // Anotación de auditoría: Spring Data asigna el usuario de la última modificación (desde AuditorAware)
    @LastModifiedBy
    @ManyToOne
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
