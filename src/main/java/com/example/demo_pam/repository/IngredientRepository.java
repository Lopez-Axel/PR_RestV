package com.example.demo_pam.repository;

import com.example.demo_pam.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    List<Ingredient> findByCurrentStockLessThanEqual(BigDecimal threshold);

    // Un método derivado no alcanza para comparar currentStock contra minimumStock de la misma fila; la regla es estrictamente menor (igual al mínimo no es stock bajo)
    @Query("select i from Ingredient i where i.currentStock < i.minimumStock")
    List<Ingredient> findLowStock();
}