package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.AddMenuDishRequest;
import com.example.demo_pam.dto.response.MenuDishResponse;
import com.example.demo_pam.service.MenuDishService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/menus/{menuId}/dishes")
public class MenuDishController {

    private final MenuDishService menuDishService;

    public MenuDishController(MenuDishService menuDishService) {
        this.menuDishService = menuDishService;
    }

    @GetMapping
    public List<MenuDishResponse> findDishesByMenu(@PathVariable UUID menuId) {
        return menuDishService.findDishesByMenu(menuId);
    }

    @PreAuthorize("hasAnyAuthority('CHEF', 'ADMIN')")
    @PostMapping
    public MenuDishResponse addDishToMenu(@PathVariable UUID menuId,
                                          @Valid @RequestBody AddMenuDishRequest request) {
        return menuDishService.addDishToMenu(menuId, request.dishId());
    }

    @PreAuthorize("hasAnyAuthority('CHEF', 'ADMIN')")
    @DeleteMapping("/{menuDishId}")
    public ResponseEntity<Void> removeDishFromMenu(@PathVariable UUID menuId,
                                                   @PathVariable UUID menuDishId) {
        menuDishService.removeDishFromMenu(menuId, menuDishId);
        return ResponseEntity.noContent().build();
    }
}