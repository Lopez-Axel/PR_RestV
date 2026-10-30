package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.IngredientRequest;
import com.example.demo_pam.dto.response.IngredientDTO;
import com.example.demo_pam.service.IngredientService;
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
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping
    public List<IngredientDTO> findAll() {
        return ingredientService.findAll();
    }

    // Alerta de stock: ingredientes cuyo stock actual está por debajo del mínimo (sin endpoint aparte)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping("/low-stock")
    public List<IngredientDTO> findLowStock() {
        return ingredientService.findLowStock();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping("/{id}")
    public IngredientDTO findById(@PathVariable UUID id) {
        return ingredientService.findById(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PostMapping
    public IngredientDTO create(@Valid @RequestBody IngredientRequest request) {
        return ingredientService.create(
                request.name(), request.unitId(), request.price(),
                request.currentStock(), request.minimumStock());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PutMapping("/{id}")
    public IngredientDTO update(@PathVariable UUID id, @Valid @RequestBody IngredientRequest request) {
        return ingredientService.update(
                id, request.name(), request.unitId(), request.price(),
                request.currentStock(), request.minimumStock());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}