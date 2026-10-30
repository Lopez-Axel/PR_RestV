package com.example.demo_pam.repository;

import com.example.demo_pam.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    Optional<StockAlert> findByIngredientId(UUID ingredientId);

    List<StockAlert> findByIngredientIdIn(Collection<UUID> ingredientIds);

    List<StockAlert> findByStatus(StockAlert.Status status);

    List<StockAlert> findByStatusIn(Collection<StockAlert.Status> statuses);

    void deleteByIngredientId(UUID ingredientId);
}
