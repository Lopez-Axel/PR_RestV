package com.example.demo_pam.repository;

import com.example.demo_pam.entity.DishIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DishIngredientRepository extends JpaRepository<DishIngredient, UUID> {

    List<DishIngredient> findByDishId(UUID dishId);

    List<DishIngredient> findByDishIdAndAvailableTrue(UUID dishId);

    // Query nativa: evita el filtro @SQLRestriction para poder reactivar una proporción borrada
    @Query(value = "SELECT * FROM dish_ingredient WHERE dish_id = :dishId AND ingredient_id = :ingredientId",
            nativeQuery = true)
    Optional<DishIngredient> findByDishIdAndIngredientIdIncludingDeleted(@Param("dishId") UUID dishId,
                                                                         @Param("ingredientId") UUID ingredientId);
}