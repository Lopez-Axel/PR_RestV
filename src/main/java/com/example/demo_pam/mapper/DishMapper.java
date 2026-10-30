package com.example.demo_pam.mapper;

import com.example.demo_pam.dto.response.DishDTO;
import com.example.demo_pam.entity.Dish;
import com.example.demo_pam.entity.User;

import java.math.BigDecimal;
import java.util.List;

public final class DishMapper {

    private DishMapper() {
    }

    public static List<DishDTO> toResponseList(List<Dish> dishes, User.Role role) {
        return dishes.stream().map(dish -> toResponse(dish, role)).toList();
    }

    // El costo y el precio de venta son información sensible: se ocultan según el rol
    public static DishDTO toResponse(Dish dish, User.Role role) {
        return switch (role) {
            case WAITER -> new DishDTO(
                    dish.getId(), dish.getName(), dish.getDescription(),
                    null, null, dish.getSellingPrice(), dish.getAvailable(),
                    null, null, null, null,
                    dish.getCreatedAt(), dish.getUpdatedAt(),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getCreatedBy()),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getUpdatedBy()));
            case CHEF -> new DishDTO(
                    dish.getId(), dish.getName(), dish.getDescription(),
                    dish.getCategory().getName(), dish.getCategory().getId(), null,
                    dish.getAvailable(), dish.getImageUrl(), null, null, null,
                    dish.getCreatedAt(), dish.getUpdatedAt(),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getCreatedBy()),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getUpdatedBy()));
            case ADMIN -> new DishDTO(
                    dish.getId(), dish.getName(), dish.getDescription(),
                    dish.getCategory().getName(), dish.getCategory().getId(),
                    dish.getSellingPrice(), dish.getAvailable(), dish.getImageUrl(),
                    dish.getProductionCost(), null, null,
                    dish.getCreatedAt(), dish.getUpdatedAt(),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getCreatedBy()),
                    com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getUpdatedBy()));
        };
    }

    public static DishDTO toResponseWithAnalysis(Dish dish, BigDecimal productionCost,
                                                 BigDecimal suggestedSellingPrice, String status) {
        return new DishDTO(
                dish.getId(), dish.getName(), dish.getDescription(),
                dish.getCategory().getName(), dish.getCategory().getId(),
                dish.getSellingPrice(), dish.getAvailable(), dish.getImageUrl(),
                productionCost, suggestedSellingPrice, status,
                dish.getCreatedAt(), dish.getUpdatedAt(),
                com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getCreatedBy()),
                com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getUpdatedBy()));
    }
}