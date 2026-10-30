package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryDTO(
        UUID id,
        String name,
        Boolean available,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static CategoryDTO from(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getAvailable(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                AuditUserDTO.from(category.getCreatedBy()),
                AuditUserDTO.from(category.getUpdatedBy())
        );
    }
}