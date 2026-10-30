package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.IngredientDTO;
import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final MeasurementUnitService measurementUnitService;
    private final StockAlertService stockAlertService;

    public IngredientService(IngredientRepository ingredientRepository,
                             MeasurementUnitService measurementUnitService,
                             StockAlertService stockAlertService) {
        this.ingredientRepository = ingredientRepository;
        this.measurementUnitService = measurementUnitService;
        this.stockAlertService = stockAlertService;
    }

    public List<IngredientDTO> findAll() {
        return ingredientRepository.findAll().stream().map(IngredientDTO::from).toList();
    }

    public List<IngredientDTO> findLowStock() {
        return ingredientRepository.findLowStock().stream().map(IngredientDTO::from).toList();
    }

    public IngredientDTO findById(UUID id) {
        return IngredientDTO.from(getIngredient(id));
    }

    @Transactional
    public IngredientDTO create(String name, UUID unitId, BigDecimal price,
                                BigDecimal currentStock, BigDecimal minimumStock) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setUnit(measurementUnitService.getUnit(unitId));
        ingredient.setPrice(price);
        ingredient.setCurrentStock(currentStock == null ? BigDecimal.ZERO : currentStock);
        ingredient.setMinimumStock(minimumStock == null ? BigDecimal.ZERO : minimumStock);
        IngredientDTO dto = IngredientDTO.from(ingredientRepository.save(ingredient));
        stockAlertService.syncAlertStates();
        return dto;
    }

    @Transactional
    public IngredientDTO update(UUID id, String name, UUID unitId, BigDecimal price,
                                BigDecimal currentStock, BigDecimal minimumStock) {
        Ingredient ingredient = getIngredient(id);
        ingredient.setName(name);
        ingredient.setUnit(measurementUnitService.getUnit(unitId));
        ingredient.setPrice(price);
        ingredient.setCurrentStock(currentStock);
        ingredient.setMinimumStock(minimumStock);
        IngredientDTO dto = IngredientDTO.from(ingredientRepository.save(ingredient));
        stockAlertService.syncAlertStates();
        return dto;
    }

    @Transactional
    public void delete(UUID id) {
        Ingredient ingredient = getIngredient(id);
        // Cierra las alertas y desvincula ítems de notificaciones antes del borrado para respetar las FK
        stockAlertService.handleIngredientRemoved(id);
        ingredientRepository.delete(ingredient);
    }

    public Ingredient getIngredient(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingrediente no encontrado: " + id));
    }
}
