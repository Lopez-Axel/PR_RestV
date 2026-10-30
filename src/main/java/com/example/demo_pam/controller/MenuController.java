package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.MenuRequest;
import com.example.demo_pam.dto.response.MenuResponse;
import com.example.demo_pam.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public List<MenuResponse> findAll() {
        return menuService.findAll();
    }

    @GetMapping("/{id}")
    public MenuResponse findById(@PathVariable UUID id) {
        return menuService.findById(id);
    }

    @PreAuthorize("hasAnyAuthority('CHEF', 'ADMIN')")
    @PostMapping
    public MenuResponse create(@Valid @RequestBody MenuRequest request) {
        return menuService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('CHEF', 'ADMIN')")
    @PutMapping("/{id}")
    public MenuResponse update(@PathVariable UUID id, @Valid @RequestBody MenuRequest request) {
        return menuService.update(id, request);
    }

    // Borrado lógico: el menú deja de listarse pero conserva su fila y sus vínculos
    @PreAuthorize("hasAnyAuthority('CHEF', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        menuService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}