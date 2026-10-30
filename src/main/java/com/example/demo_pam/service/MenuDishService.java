package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.MenuDishResponse;
import com.example.demo_pam.entity.Dish;
import com.example.demo_pam.entity.Menu;
import com.example.demo_pam.entity.MenuDish;
import com.example.demo_pam.exception.BusinessRuleViolationException;
import com.example.demo_pam.exception.InvalidOrderException;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.DishRepository;
import com.example.demo_pam.repository.MenuDishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuDishService {

    private final MenuDishRepository menuDishRepository;
    private final MenuService menuService;
    private final DishRepository dishRepository;

    public MenuDishService(MenuDishRepository menuDishRepository,
                           MenuService menuService,
                           DishRepository dishRepository) {
        this.menuDishRepository = menuDishRepository;
        this.menuService = menuService;
        this.dishRepository = dishRepository;
    }

    @Transactional
    public MenuDishResponse addDishToMenu(UUID menuId, UUID dishId) {
        Menu menu = menuService.getMenu(menuId);
        if (!Boolean.TRUE.equals(menu.getActive())) {
            throw new BusinessRuleViolationException(
                    "El menú " + menu.getName() + " no está activo para agregar platos");
        }
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Plato no encontrado: " + dishId));
        if (menuDishRepository.findByMenuIdAndDishId(menuId, dishId).isPresent()) {
            throw new BusinessRuleViolationException("El plato " + dish.getName() + " ya está en el menú");
        }
        MenuDish menuDish = new MenuDish();
        menuDish.setMenu(menu);
        menuDish.setDish(dish);
        return MenuDishResponse.from(menuDishRepository.save(menuDish));
    }

    @Transactional
    public void removeDishFromMenu(UUID menuId, UUID menuDishId) {
        MenuDish menuDish = menuDishRepository.findById(menuDishId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de menú no encontrado: " + menuDishId));
        if (!menuDish.getMenu().getId().equals(menuId)) {
            throw new ResourceNotFoundException("Vínculo de menú no encontrado: " + menuDishId);
        }
        menuDishRepository.delete(menuDish);
    }

    public List<MenuDishResponse> findDishesByMenu(UUID menuId) {
        return menuDishRepository.findByMenuIdOrderByCreatedAtAsc(menuId)
                .stream().map(MenuDishResponse::from).toList();
    }

    public MenuDish findDishInActiveMenu(UUID dishId) {
        return menuDishRepository.findByDishIdAndMenuActiveTrue(dishId).stream()
                .findFirst()
                .orElseThrow(() -> new InvalidOrderException(
                        "El plato no pertenece a un menú activo"));
    }

    @Transactional
    public void removeDishFromAllMenus(UUID dishId) {
        menuDishRepository.deleteByDishId(dishId);
    }
}
