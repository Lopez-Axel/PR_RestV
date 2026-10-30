package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.Menu;

import java.time.Instant;
import java.util.UUID;

public record MenuResponse(
        UUID id,
        String name,
        String description,
        Boolean active,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getActive(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                AuditUserDTO.from(menu.getCreatedBy()),
                AuditUserDTO.from(menu.getUpdatedBy())
        );
    }
}