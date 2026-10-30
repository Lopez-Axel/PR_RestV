package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.StockAlertDTO;
import com.example.demo_pam.dto.response.StockAlertOverviewDTO;
import com.example.demo_pam.dto.response.StockNotificationDTO;
import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.StockAlert;
import com.example.demo_pam.entity.StockNotification;
import com.example.demo_pam.repository.IngredientRepository;
import com.example.demo_pam.repository.StockAlertRepository;
import com.example.demo_pam.repository.StockNotificationItemRepository;
import com.example.demo_pam.repository.StockNotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockAlertService {

    private final IngredientRepository ingredientRepository;
    private final StockAlertRepository stockAlertRepository;
    private final StockNotificationRepository stockNotificationRepository;
    private final StockNotificationItemRepository stockNotificationItemRepository;
    private final long reminderIntervalMinutes;

    public StockAlertService(IngredientRepository ingredientRepository,
                             StockAlertRepository stockAlertRepository,
                             StockNotificationRepository stockNotificationRepository,
                             StockNotificationItemRepository stockNotificationItemRepository,
                             @Value("${app.stock-alerts.reminder-interval-minutes:10080}") long reminderIntervalMinutes) {
        this.ingredientRepository = ingredientRepository;
        this.stockAlertRepository = stockAlertRepository;
        this.stockNotificationRepository = stockNotificationRepository;
        this.stockNotificationItemRepository = stockNotificationItemRepository;
        this.reminderIntervalMinutes = reminderIntervalMinutes;
    }

    // Transaccional: el estado de las alertas se actualiza atómicamente junto con la modificación del ingrediente que lo invoca
    @Transactional
    public void syncAlertStates() {
        Instant now = Instant.now();
        List<Ingredient> lowStockIngredients = ingredientRepository.findLowStock();
        Set<UUID> lowStockIds = lowStockIngredients.stream()
                .map(Ingredient::getId)
                .collect(Collectors.toSet());

        Map<UUID, StockAlert> alertsByIngredient = lowStockIds.isEmpty()
                ? Map.of()
                : stockAlertRepository.findByIngredientIdIn(lowStockIds).stream()
                        .collect(Collectors.toMap(a -> a.getIngredient().getId(), a -> a));

        for (Ingredient ingredient : lowStockIngredients) {
            StockAlert alert = alertsByIngredient.get(ingredient.getId());
            if (alert == null) {
                alert = new StockAlert();
                alert.setIngredient(ingredient);
                alert.setStatus(StockAlert.Status.OPEN);
                alert.setDetectedAt(now);
                stockAlertRepository.save(alert);
            } else if (alert.getStatus() == StockAlert.Status.RESOLVED) {
                alert.setStatus(StockAlert.Status.OPEN);
                alert.setDetectedAt(now);
                alert.setResolvedAt(null);
                stockAlertRepository.save(alert);
            } else if (alert.getStatus() == StockAlert.Status.NOTIFIED
                    && shouldNotifyAgain(ingredient, alert, now)) {
                alert.setStatus(StockAlert.Status.OPEN);
                alert.setDetectedAt(now);
                stockAlertRepository.save(alert);
            }
        }

        for (StockAlert alert : stockAlertRepository.findByStatusIn(
                List.of(StockAlert.Status.OPEN, StockAlert.Status.NOTIFIED))) {
            if (!lowStockIds.contains(alert.getIngredient().getId())) {
                alert.setStatus(StockAlert.Status.RESOLVED);
                alert.setResolvedAt(now);
                stockAlertRepository.save(alert);
            }
        }
    }

    // Transaccional: cierra las alertas y desvincula los ítems históricos antes del borrado del ingrediente
    @Transactional
    public void handleIngredientRemoved(UUID ingredientId) {
        stockNotificationItemRepository.detachByIngredientId(ingredientId);
        stockAlertRepository.deleteByIngredientId(ingredientId);
    }

    @Transactional(readOnly = true)
    public StockAlertOverviewDTO getOverview() {
        List<StockAlertDTO> alerts = stockAlertRepository.findAll(Sort.by(Sort.Direction.DESC, "detectedAt"))
                .stream()
                .map(StockAlertDTO::from)
                .toList();
        List<StockNotificationDTO> notifications = stockNotificationRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(n -> StockNotificationDTO.from(
                        n, (int) stockNotificationItemRepository.countByNotificationId(n.getId())))
                .toList();
        return new StockAlertOverviewDTO(alerts, notifications);
    }

    private boolean shouldNotifyAgain(Ingredient ingredient, StockAlert alert, Instant now) {
        boolean stockChanged = alert.getNotifiedCurrentStock() == null
                || alert.getNotifiedMinimumStock() == null
                || !ingredient.getCurrentStock().equals(alert.getNotifiedCurrentStock())
                || !ingredient.getMinimumStock().equals(alert.getNotifiedMinimumStock());
        boolean reminderDue = alert.getNotifiedAt() != null
                && !alert.getNotifiedAt().plus(reminderIntervalMinutes, ChronoUnit.MINUTES).isAfter(now);
        return stockChanged || reminderDue;
    }
}
