package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.DishRequest;
import com.example.demo_pam.dto.request.SellingPriceRequest;
import com.example.demo_pam.dto.response.CostAnalysisDTO;
import com.example.demo_pam.dto.response.DishDTO;
import com.example.demo_pam.dto.response.DishSummaryDTO;
import com.example.demo_pam.entity.Dish;
import com.example.demo_pam.entity.DishIngredient;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.mapper.DishIngredientMapper;
import com.example.demo_pam.mapper.DishMapper;
import com.example.demo_pam.repository.DishIngredientRepository;
import com.example.demo_pam.repository.DishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final CategoryService categoryService;
    private final PricingService pricingService;
    private final MenuDishService menuDishService;

    public DishService(DishRepository dishRepository,
                       DishIngredientRepository dishIngredientRepository,
                       CategoryService categoryService,
                       PricingService pricingService,
                       MenuDishService menuDishService) {
        this.dishRepository = dishRepository;
        this.dishIngredientRepository = dishIngredientRepository;
        this.categoryService = categoryService;
        this.pricingService = pricingService;
        this.menuDishService = menuDishService;
    }

    public List<DishDTO> findAll(User.Role role, boolean analysis) {
        List<Dish> dishes = dishRepository.findAll();
        if (analysis) {
            return dishes.stream().map(this::toAnalysisDto).toList();
        }
        return DishMapper.toResponseList(dishes, role);
    }

    public DishDTO findById(UUID id, User.Role role) {
        return DishMapper.toResponse(getDish(id), role);
    }

    @Transactional
    public DishDTO create(DishRequest request, User.Role role) {
        Dish dish = new Dish();
        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setCategory(categoryService.getCategory(request.categoryId()));
        dish.setAvailable(request.available() == null ? Boolean.FALSE : request.available());
        dish.setProductionCost(request.productionCost() == null ? BigDecimal.ZERO : request.productionCost());
        dish.setImageUrl(request.imageUrl());
        BigDecimal price = role == User.Role.CHEF ? null : request.sellingPrice();
        dish.setSellingPrice(price);
        requirePriceIfAvailable(dish);
        return DishMapper.toResponse(dishRepository.save(dish), role);
    }

    @Transactional
    public DishDTO update(UUID id, DishRequest request, User.Role role) {
        Dish dish = getDish(id);
        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setCategory(categoryService.getCategory(request.categoryId()));
        if (request.available() != null) {
            boolean wasAvailable = Boolean.TRUE.equals(dish.getAvailable());
            boolean willBeAvailable = Boolean.TRUE.equals(request.available());
            dish.setAvailable(request.available());
            if (wasAvailable && !willBeAvailable) {
                menuDishService.removeDishFromAllMenus(dish.getId());
            }
        }
        if (request.imageUrl() != null) {
            dish.setImageUrl(request.imageUrl());
        }
        if (request.productionCost() != null) {
            dish.setProductionCost(request.productionCost());
        }
        if (role == User.Role.ADMIN && request.sellingPrice() != null) {
            dish.setSellingPrice(request.sellingPrice());
        }
        requirePriceIfAvailable(dish);
        return DishMapper.toResponse(dishRepository.save(dish), role);
    }

    // Soft delete: el plato deja de listarse pero conserva su fila por auditoría
    @Transactional
    public void softDelete(UUID id) {
        Dish dish = getDish(id);
        dish.setDeleted(true);
        dishRepository.save(dish);
    }

    public CostAnalysisDTO costAnalysis(UUID id) {
        return pricingService.analyze(getDish(id));
    }

    // Recalcula el costo de producción y lo persiste como acción de UI
    @Transactional
    public CostAnalysisDTO recomputeProductionCost(UUID id) {
        Dish dish = getDish(id);
        dish.setProductionCost(pricingService.calculateProductionCost(dish));
        dishRepository.save(dish);
        return pricingService.analyze(dish);
    }

    @Transactional
    public DishDTO updateSellingPrice(UUID id, SellingPriceRequest request) {
        Dish dish = getDish(id);
        dish.setSellingPrice(request.sellingPrice());
        requirePriceIfAvailable(dish);
        return DishMapper.toResponse(dishRepository.save(dish), User.Role.ADMIN);
    }

    public DishSummaryDTO summary(UUID id) {
        Dish dish = getDish(id);
        BigDecimal productionCost = pricingService.calculateProductionCost(dish);
        BigDecimal suggested = pricingService.suggestedSellingPrice(productionCost);
        BigDecimal sellingPrice = dish.getSellingPrice();
        List<DishIngredient> ingredients = dishIngredientRepository.findByDishIdAndAvailableTrue(dish.getId());
        return new DishSummaryDTO(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getCategory().getName(),
                dish.getAvailable(),
                dish.getImageUrl(),
                productionCost,
                suggested,
                sellingPrice,
                pricingService.marginPercent(productionCost, sellingPrice),
                pricingService.status(productionCost, sellingPrice),
                DishIngredientMapper.toResponseList(ingredients),
                dish.getCreatedAt(),
                dish.getUpdatedAt(),
                com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getCreatedBy()),
                com.example.demo_pam.dto.response.AuditUserDTO.from(dish.getUpdatedBy())
        );
    }

    @Transactional
    public DishDTO setImage(UUID id, String imageUrl) {
        Dish dish = getDish(id);
        dish.setImageUrl(imageUrl);
        return DishMapper.toResponse(dishRepository.save(dish), User.Role.ADMIN);
    }

    public Dish getDish(UUID id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plato no encontrado: " + id));
    }

    private DishDTO toAnalysisDto(Dish dish) {
        BigDecimal productionCost = pricingService.calculateProductionCost(dish);
        return DishMapper.toResponseWithAnalysis(
                dish, productionCost, pricingService.suggestedSellingPrice(productionCost),
                pricingService.status(productionCost, dish.getSellingPrice()));
    }

    // Regla de negocio: un plato disponible siempre debe tener precio de venta
    private void requirePriceIfAvailable(Dish dish) {
        if (Boolean.TRUE.equals(dish.getAvailable()) && dish.getSellingPrice() == null) {
            throw new BusinessRuleViolationException(
                    "No se puede marcar el plato " + dish.getName() + " como disponible sin precio de venta");
        }
    }
}