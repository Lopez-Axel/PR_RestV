package com.example.demo_pam.service;

import com.example.demo_pam.dto.request.MenuRequest;
import com.example.demo_pam.dto.response.MenuResponse;
import com.example.demo_pam.entity.Menu;
import com.example.demo_pam.exception.ResourceNotFoundException;
import com.example.demo_pam.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MenuService {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    // Lista todos los menús no eliminados (deleted = false)
    public List<MenuResponse> findAll() {
        return menuRepository.findAll().stream().map(MenuResponse::from).toList();
    }

    public MenuResponse findById(UUID id) {
        return MenuResponse.from(getMenu(id));
    }

    @Transactional
    public MenuResponse create(MenuRequest request) {
        Menu menu = new Menu();
        menu.setName(request.name());
        menu.setDescription(request.description());
        menu.setActive(request.active() == null ? Boolean.TRUE : request.active());
        return MenuResponse.from(menuRepository.save(menu));
    }

    @Transactional
    public MenuResponse update(UUID id, MenuRequest request) {
        Menu menu = getMenu(id);
        menu.setName(request.name());
        menu.setDescription(request.description());
        if (request.active() != null) {
            menu.setActive(request.active());
        }
        return MenuResponse.from(menuRepository.save(menu));
    }

    // Borrado lógico: el menú deja de listarse pero conserva su fila y sus vínculos por auditoría
    @Transactional
    public void softDelete(UUID id) {
        Menu menu = getMenu(id);
        menu.setDeleted(true);
        menuRepository.save(menu);
    }

    public Menu getMenu(UUID id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menú no encontrado: " + id));
    }
}