package com.example.demo_pam.controller;

import com.example.demo_pam.dto.request.DishIngredientRequest;
import com.example.demo_pam.dto.request.DishRequest;
import com.example.demo_pam.dto.request.SellingPriceRequest;
import com.example.demo_pam.dto.response.CostAnalysisDTO;
import com.example.demo_pam.dto.response.DishDTO;
import com.example.demo_pam.dto.response.DishIngredientDTO;
import com.example.demo_pam.dto.response.DishSummaryDTO;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.security.AppUserPrincipal;
import com.example.demo_pam.service.DishIngredientService;
import com.example.demo_pam.service.DishService;
import com.example.demo_pam.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService dishService;
    private final DishIngredientService dishIngredientService;
    private final ImageStorageService imageStorageService;

    public DishController(DishService dishService,
                          DishIngredientService dishIngredientService,
                          ImageStorageService imageStorageService) {
        this.dishService = dishService;
        this.dishIngredientService = dishIngredientService;
        this.imageStorageService = imageStorageService;
    }

    // El modo análisis (con precios y costos) solo lo puede pedir un ADMIN
    @GetMapping
    public List<DishDTO> findAll(@RequestParam(defaultValue = "false") boolean analysis,
                                 @AuthenticationPrincipal AppUserPrincipal principal) {
        if (analysis && principal.getUser().getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("El análisis de costos es exclusivo de administradores");
        }
        return dishService.findAll(principal.getUser().getRole(), analysis);
    }

    @GetMapping("/{id}")
    public DishDTO findById(@PathVariable UUID id,
                            @AuthenticationPrincipal AppUserPrincipal principal) {
        return dishService.findById(id, principal.getUser().getRole());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PostMapping
    public DishDTO create(@Valid @RequestBody DishRequest request,
                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return dishService.create(request, principal.getUser().getRole());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PutMapping("/{id}")
    public DishDTO update(@PathVariable UUID id, @Valid @RequestBody DishRequest request,
                          @AuthenticationPrincipal AppUserPrincipal principal) {
        return dishService.update(id, request, principal.getUser().getRole());
    }

    // Soft delete: el plato deja de listarse pero conserva su fila por auditoría
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dishService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @GetMapping("/{id}/ingredients")
    public List<DishIngredientDTO> findIngredients(@PathVariable UUID id) {
        return dishIngredientService.findByDishId(id);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PostMapping("/{id}/ingredients")
    public DishIngredientDTO addIngredient(@PathVariable UUID id,
                                           @Valid @RequestBody DishIngredientRequest request) {
        return dishIngredientService.add(id, request);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @PutMapping("/{id}/ingredients/{dishIngredientId}")
    public DishIngredientDTO updateIngredient(@PathVariable UUID id,
                                              @PathVariable UUID dishIngredientId,
                                              @Valid @RequestBody DishIngredientRequest request) {
        return dishIngredientService.update(id, dishIngredientId, request);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CHEF')")
    @DeleteMapping("/{id}/ingredients/{dishIngredientId}")
    public ResponseEntity<Void> deleteIngredient(@PathVariable UUID id,
                                                 @PathVariable UUID dishIngredientId) {
        dishIngredientService.softDelete(id, dishIngredientId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/cost-analysis")
    public CostAnalysisDTO costAnalysis(@PathVariable UUID id) {
        return dishService.costAnalysis(id);
    }

    // Recalcula el costo de producción y lo persiste como acción manual del ADMIN
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/production-cost")
    public CostAnalysisDTO recomputeProductionCost(@PathVariable UUID id) {
        return dishService.recomputeProductionCost(id);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/{id}/selling-price")
    public DishDTO updateSellingPrice(@PathVariable UUID id,
                                      @Valid @RequestBody SellingPriceRequest request) {
        return dishService.updateSellingPrice(id, request);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/summary")
    public DishSummaryDTO summary(@PathVariable UUID id) {
        return dishService.summary(id);
    }

    // Guarda la imagen en disco y adjunta su URL al plato
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DishDTO uploadImage(@PathVariable UUID id,
                               @RequestPart("file") MultipartFile file) {
        return dishService.setImage(id, imageStorageService.store(file));
    }
}