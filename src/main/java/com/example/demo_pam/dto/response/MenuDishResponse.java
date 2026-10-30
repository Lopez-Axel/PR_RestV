package com.example.demo_pam.dto.response;

import com.example.demo_pam.entity.MenuDish;
import com.example.demo_pam.entity.User;

import java.time.Instant;
import java.util.UUID;

public record MenuDishResponse(
        UUID id,
        UUID menuId,
        UUID dishId,
        String dishName,
        String addedByName,
        Instant addedAt,
        Instant createdAt,
        Instant updatedAt,
        AuditUserDTO createdBy,
        AuditUserDTO updatedBy
) {

    public static MenuDishResponse from(MenuDish menuDish) {
        User addedBy = menuDish.getCreatedBy();
        String addedByName = addedBy == null ? null
                : (addedBy.getFirstName() + " " + addedBy.getLastName()).trim();
        return new MenuDishResponse(
                menuDish.getId(),
                menuDish.getMenu().getId(),
                menuDish.getDish().getId(),
                menuDish.getDish().getName(),
                addedByName,
                menuDish.getCreatedAt(),
                menuDish.getCreatedAt(),
                menuDish.getUpdatedAt(),
                AuditUserDTO.from(menuDish.getCreatedBy()),
                AuditUserDTO.from(menuDish.getUpdatedBy())
        );
    }
}