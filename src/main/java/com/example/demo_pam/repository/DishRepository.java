package com.example.demo_pam.repository;

import com.example.demo_pam.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DishRepository extends JpaRepository<Dish, UUID> {

    List<Dish> findByAvailableTrue();

    List<Dish> findByCategoryId(UUID categoryId);

    List<Dish> findByCategoryIdAndAvailableTrue(UUID categoryId);
}