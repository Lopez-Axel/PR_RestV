package com.example.demo_pam.repository;

import com.example.demo_pam.entity.StockNotificationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockNotificationItemRepository extends JpaRepository<StockNotificationItem, UUID> {

    List<StockNotificationItem> findByNotificationId(UUID notificationId);

    long countByNotificationId(UUID notificationId);

    // Desvincula el ingrediente de los ítems históricos antes de borrarlo (ON DELETE no está definido a nivel de BD)
    @Modifying
    @Query("update StockNotificationItem i set i.ingredient = null where i.ingredient.id = :ingredientId")
    void detachByIngredientId(@Param("ingredientId") UUID ingredientId);
}
