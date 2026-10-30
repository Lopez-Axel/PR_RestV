package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.CategoryDTO;
import com.example.demo_pam.entity.Category;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Solo expone categorías no eliminadas (soft delete)
    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllNotDeleted() {
        return categoryRepository.findByDeletedFalse().stream().map(CategoryDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO findById(UUID id) {
        Category category = getCategory(id);
        if (!category.getAvailable()) {
            throw new ResourceNotFoundException("Categoría no encontrada: " + id);
        }
        return CategoryDTO.from(category);
    }

    @Transactional
    public CategoryDTO create(String name, Boolean available) {
        Category category = new Category();
        category.setName(name);
        if (available != null) {
            category.setAvailable(available);
        }
        category = categoryRepository.save(category);
        categoryRepository.flush();
        return CategoryDTO.from(category);
    }

    @Transactional
    public CategoryDTO update(UUID id, String name, Boolean available) {
        Category category = getCategory(id);
        category.setName(name);
        if (available != null) {
            category.setAvailable(available);
        }
        category = categoryRepository.save(category);
        categoryRepository.flush();
        return CategoryDTO.from(category);
    }

    // Soft delete: la categoría deja de listarse pero conserva su fila por auditoría
    @Transactional
    public void delete(UUID id) {
        Category category = getCategory(id);
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }
}