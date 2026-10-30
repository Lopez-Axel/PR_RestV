package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.DishIngredientRequest;
import com.example.demo_pam.dto.response.DishIngredientDTO;
import com.example.demo_pam.entity.Dish;
import com.example.demo_pam.entity.DishIngredient;
import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.mapper.DishIngredientMapper;
import com.example.demo_pam.repository.DishIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DishIngredientService {

    private final DishIngredientRepository dishIngredientRepository;
    private final DishService dishService;
    private final IngredientService ingredientService;
    private final MeasurementUnitService measurementUnitService;

    public DishIngredientService(DishIngredientRepository dishIngredientRepository,
                                 DishService dishService,
                                 IngredientService ingredientService,
                                 MeasurementUnitService measurementUnitService) {
        this.dishIngredientRepository = dishIngredientRepository;
        this.dishService = dishService;
        this.ingredientService = ingredientService;
        this.measurementUnitService = measurementUnitService;
    }

    public List<DishIngredientDTO> findByDishId(UUID dishId) {
        return DishIngredientMapper.toResponseList(dishIngredientRepository.findByDishIdAndAvailableTrue(dishId));
    }

    // Valida que el tipo de unidad elegido coincida con el tipo de unidad del ingrediente
    @Transactional
    public DishIngredientDTO add(UUID dishId, DishIngredientRequest request) {
        Dish dish = dishService.getDish(dishId);
        Ingredient ingredient = ingredientService.getIngredient(request.ingredientId());
        MeasurementUnit unit = measurementUnitService.getUnit(request.unitId());
        requireSameUnitType(ingredient, unit);

        DishIngredient existing = dishIngredientRepository
                .findByDishIdAndIngredientIdIncludingDeleted(dishId, request.ingredientId())
                .orElse(null);
        if (existing != null && !existing.getDeleted()) {
            throw new BusinessRuleViolationException(
                    "El ingrediente ya está cargado en el plato: " + ingredient.getName());
        }
        if (existing != null) {
            existing.setDeleted(false);
            existing.setAvailable(true);
            existing.setEstimatedQuantity(request.estimatedQuantity());
            existing.setUnit(unit);
            return DishIngredientDTO.from(dishIngredientRepository.save(existing));
        }
        DishIngredient dishIngredient = new DishIngredient();
        dishIngredient.setDish(dish);
        dishIngredient.setIngredient(ingredient);
        dishIngredient.setEstimatedQuantity(request.estimatedQuantity());
        dishIngredient.setUnit(unit);
        return DishIngredientDTO.from(dishIngredientRepository.save(dishIngredient));
    }

    @Transactional
    public DishIngredientDTO update(UUID dishId, UUID dishIngredientId, DishIngredientRequest request) {
        DishIngredient dishIngredient = getDishIngredient(dishId, dishIngredientId);
        MeasurementUnit unit = measurementUnitService.getUnit(request.unitId());
        requireSameUnitType(dishIngredient.getIngredient(), unit);
        if (request.ingredientId() != null && !request.ingredientId().equals(dishIngredient.getIngredient().getId())) {
            throw new BusinessRuleViolationException("No se puede cambiar el ingrediente de una proporción existente");
        }
        dishIngredient.setEstimatedQuantity(request.estimatedQuantity());
        dishIngredient.setUnit(unit);
        return DishIngredientDTO.from(dishIngredientRepository.save(dishIngredient));
    }

    // Soft delete de la asociación: la proporción deja de usarse pero conserva su fila
    @Transactional
    public void softDelete(UUID dishId, UUID dishIngredientId) {
        DishIngredient dishIngredient = getDishIngredient(dishId, dishIngredientId);
        dishIngredient.setDeleted(true);
        dishIngredientRepository.save(dishIngredient);
    }

    public List<DishIngredient> findEntitiesByDishId(UUID dishId) {
        return dishIngredientRepository.findByDishIdAndAvailableTrue(dishId);
    }

    private DishIngredient getDishIngredient(UUID dishId, UUID dishIngredientId) {
        DishIngredient dishIngredient = dishIngredientRepository.findById(dishIngredientId)
                .orElseThrow(() -> new ResourceNotFoundException("Proporción de ingrediente no encontrada: " + dishIngredientId));
        if (!dishIngredient.getDish().getId().equals(dishId)) {
            throw new ResourceNotFoundException("Proporción de ingrediente no encontrada: " + dishIngredientId);
        }
        return dishIngredient;
    }

    private void requireSameUnitType(Ingredient ingredient, MeasurementUnit unit) {
        if (unit.getUnitType() != ingredient.getUnit().getUnitType()) {
            throw new IllegalArgumentException(
                    "La unidad " + unit.getName() + " no corresponde al tipo del ingrediente " + ingredient.getName());
        }
    }
}