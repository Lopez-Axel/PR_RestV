package com.example.demo_pam.service;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private MeasurementUnitService measurementUnitService;
    @Mock
    private StockAlertService stockAlertService;

    @InjectMocks
    private IngredientService ingredientService;

    private Ingredient sampleIngredient() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(UUID.randomUUID());
        ingredient.setName("Harina");
        ingredient.setCurrentStock(new BigDecimal("2.00"));
        ingredient.setMinimumStock(new BigDecimal("5.00"));
        ingredient.setPrice(new BigDecimal("1.50"));
        MeasurementUnit unit = new MeasurementUnit();
        unit.setAbbreviation("kg");
        ingredient.setUnit(unit);
        return ingredient;
    }

    @Test
    void findAll_doesNotTriggerAlertLogicOrEmails() {
        when(ingredientRepository.findAll()).thenReturn(List.of(sampleIngredient()));

        List<?> result = ingredientService.findAll();

        assertThat(result).hasSize(1);
        verifyNoInteractions(stockAlertService);
    }

    @Test
    void findLowStock_doesNotTriggerAlertLogicOrEmails() {
        when(ingredientRepository.findLowStock()).thenReturn(List.of(sampleIngredient()));

        List<?> result = ingredientService.findLowStock();

        assertThat(result).hasSize(1);
        verifyNoInteractions(stockAlertService);
    }

    @Test
    void findById_doesNotTriggerAlertLogicOrEmails() {
        Ingredient ingredient = sampleIngredient();
        when(ingredientRepository.findById(ingredient.getId())).thenReturn(java.util.Optional.of(ingredient));

        ingredientService.findById(ingredient.getId());

        verifyNoInteractions(stockAlertService);
    }

    @Test
    void create_synchronizesAlertStates() {
        Ingredient ingredient = sampleIngredient();
        ingredient.setId(null);
        when(measurementUnitService.getUnit(any(UUID.class))).thenReturn(ingredient.getUnit());
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> {
            Ingredient saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ingredientService.create("Harina", UUID.randomUUID(), new BigDecimal("1.50"),
                new BigDecimal("2.00"), new BigDecimal("5.00"));

        verify(stockAlertService).syncAlertStates();
    }

    @Test
    void update_synchronizesAlertStates() {
        Ingredient ingredient = sampleIngredient();
        when(ingredientRepository.findById(ingredient.getId())).thenReturn(java.util.Optional.of(ingredient));
        when(measurementUnitService.getUnit(any(UUID.class))).thenReturn(ingredient.getUnit());
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(inv -> inv.getArgument(0));

        ingredientService.update(ingredient.getId(), "Harina", UUID.randomUUID(),
                new BigDecimal("1.50"), new BigDecimal("10.00"), new BigDecimal("5.00"));

        verify(stockAlertService).syncAlertStates();
        assertThat(ingredient.getCurrentStock()).isEqualByComparingTo("10.00");
    }

    @Test
    void delete_closesAlertsBeforeRemovingIngredient() {
        Ingredient ingredient = sampleIngredient();
        UUID id = ingredient.getId();
        when(ingredientRepository.findById(id)).thenReturn(java.util.Optional.of(ingredient));

        ingredientService.delete(id);

        InOrder inOrder = inOrder(stockAlertService, ingredientRepository);
        inOrder.verify(stockAlertService).handleIngredientRemoved(id);
        inOrder.verify(ingredientRepository).delete(ingredient);
    }
}
