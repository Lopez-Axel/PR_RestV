package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.User;

import java.util.UUID;

// Actor mínimo de auditoría: evita exponer la entidad JPA User completa
public record AuditUserDTO(
        UUID id,
        String email,
        String firstName,
        String lastName
) {

    public static AuditUserDTO from(User user) {
        if (user == null) {
            return null;
        }
        return new AuditUserDTO(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}