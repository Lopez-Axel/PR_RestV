package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.CostAnalysisDTO;
import com.example.demo_pam.entity.BusinessSettings;
import com.example.demo_pam.entity.Dish;
import com.example.demo_pam.entity.DishIngredient;
import com.example.demo_pam.repository.DishIngredientRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final DishIngredientRepository dishIngredientRepository;
    private final UnitConversionService unitConversionService;
    private final BusinessSettingsService businessSettingsService;

    public PricingService(DishIngredientRepository dishIngredientRepository,
                          UnitConversionService unitConversionService,
                          BusinessSettingsService businessSettingsService) {
        this.dishIngredientRepository = dishIngredientRepository;
        this.unitConversionService = unitConversionService;
        this.businessSettingsService = businessSettingsService;
    }

    // Convierte cada proporción a la unidad base de su ingrediente y acumula precio × cantidad
	public BigDecimal calculateProductionCost(Dish dish) {
		List<DishIngredient> items = dishIngredientRepository.findByDishIdAndAvailableTrue(dish.getId());
		BigDecimal cost = BigDecimal.ZERO;
		for (DishIngredient item : items) {
			BigDecimal converted = unitConversionService.convert(
					item.getEstimatedQuantity(), item.getUnit(), item.getIngredient().getUnit());
			BigDecimal lineCost = item.getIngredient().getPrice().multiply(converted);

			// LOG TEMPORAL
			System.out.printf("%s | receta=%s %s | base=%s | precio=%s | converted=%s | lineCost=%s%n",
					item.getIngredient().getName(),
					item.getEstimatedQuantity(), item.getUnit().getName(),
					item.getIngredient().getUnit().getName(),
					item.getIngredient().getPrice(),
					converted, lineCost);

			cost = cost.add(lineCost);
		}
		System.out.println("COSTO TOTAL = " + cost);
		return cost.setScale(2, RoundingMode.HALF_UP);
	}

    // Precio sugerido con markup sobre costo: costo × (1 + margen por defecto)
    public BigDecimal suggestedSellingPrice(BigDecimal productionCost) {
        BusinessSettings settings = businessSettingsService.get();
        BigDecimal marginFactor = settings.getDefaultMarginPercentage().divide(HUNDRED, 6, RoundingMode.HALF_UP);
        return productionCost.multiply(BigDecimal.ONE.add(marginFactor)).setScale(2, RoundingMode.HALF_UP);
    }

    // Clasifica el precio según los umbrales configurables de márgenes
    public String status(BigDecimal productionCost, BigDecimal sellingPrice) {
        if (sellingPrice == null) {
            return "NO_PRICE";
        }
        if (sellingPrice.compareTo(productionCost) < 0) {
            return "LOSS";
        }
        BusinessSettings settings = businessSettingsService.get();
        BigDecimal min = productionCost.multiply(BigDecimal.ONE.add(
                settings.getMinMarginPercentage().divide(HUNDRED, 6, RoundingMode.HALF_UP)));
        BigDecimal max = productionCost.multiply(BigDecimal.ONE.add(
                settings.getMaxMarginPercentage().divide(HUNDRED, 6, RoundingMode.HALF_UP)));
        if (sellingPrice.compareTo(max) > 0) {
            return "OVERPRICED";
        }
        if (sellingPrice.compareTo(min) >= 0) {
            return "PRICED";
        }
        return "LOW_MARGIN";
    }

    public BigDecimal marginPercent(BigDecimal productionCost, BigDecimal sellingPrice) {
        if (sellingPrice == null || productionCost.signum() == 0) {
            return null;
        }
        return sellingPrice.subtract(productionCost)
                .divide(productionCost, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public CostAnalysisDTO analyze(Dish dish) {
        BigDecimal productionCost = calculateProductionCost(dish);
        return new CostAnalysisDTO(
                dish.getId(),
                dish.getName(),
                productionCost,
                suggestedSellingPrice(productionCost),
                dish.getSellingPrice(),
                status(productionCost, dish.getSellingPrice())
        );
    }
}
