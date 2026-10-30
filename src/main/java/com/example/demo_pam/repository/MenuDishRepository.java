package com.example.demo_pam.repository;

import com.example.demo_pam.entity.MenuDish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuDishRepository extends JpaRepository<MenuDish, UUID> {

    List<MenuDish> findByMenuIdOrderByCreatedAtAsc(UUID menuId);

    Optional<MenuDish> findByMenuIdAndDishId(UUID menuId, UUID dishId);

    // Solo los vínculos sin menús borrados o fuera de servicio (usado al validar un pedido)
    List<MenuDish> findByDishIdAndMenuActiveTrue(UUID dishId);

    List<MenuDish> findByDishId(UUID dishId);

    @Modifying
    @Query("delete from MenuDish md where md.dish.id = :dishId")
    void deleteByDishId(@Param("dishId") UUID dishId);
}