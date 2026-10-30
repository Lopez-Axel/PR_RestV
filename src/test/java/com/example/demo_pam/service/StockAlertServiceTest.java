package com.example.demo_pam.service;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.entity.StockAlert;
import com.example.demo_pam.repository.IngredientRepository;
import com.example.demo_pam.repository.StockAlertRepository;
import com.example.demo_pam.repository.StockNotificationItemRepository;
import com.example.demo_pam.repository.StockNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private StockAlertRepository stockAlertRepository;
    @Mock
    private StockNotificationRepository stockNotificationRepository;
    @Mock
    private StockNotificationItemRepository stockNotificationItemRepository;

    private StockAlertService stockAlertService;

    @BeforeEach
    void setUp() {
        stockAlertService = new StockAlertService(
                ingredientRepository, stockAlertRepository,
                stockNotificationRepository, stockNotificationItemRepository, 10080);
    }

    private Ingredient ingredient(UUID id, String currentStock, String minimumStock) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName("Ingrediente " + id);
        ingredient.setCurrentStock(new BigDecimal(currentStock));
        ingredient.setMinimumStock(new BigDecimal(minimumStock));
        MeasurementUnit unit = new MeasurementUnit();
        unit.setAbbreviation("kg");
        ingredient.setUnit(unit);
        return ingredient;
    }

    private StockAlert alertFor(Ingredient ingredient, StockAlert.Status status) {
        StockAlert alert = new StockAlert();
        alert.setIngredient(ingredient);
        alert.setStatus(status);
        alert.setDetectedAt(Instant.now());
        return alert;
    }

    @Test
    void syncAlertStates_currentStockBelowMinimum_createsOpenAlert() {
        Ingredient low = ingredient(UUID.randomUUID(), "2.00", "5.00");
        when(ingredientRepository.findLowStock()).thenReturn(List.of(low));
        when(stockAlertRepository.findByIngredientIdIn(anyCollection())).thenReturn(List.of());
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of());

        stockAlertService.syncAlertStates();

        ArgumentCaptor<StockAlert> captor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StockAlert.Status.OPEN);
        assertThat(captor.getValue().getIngredient()).isSameAs(low);
        assertThat(captor.getValue().getDetectedAt()).isNotNull();
    }

    @Test
    void syncAlertStates_stockEqualToMinimum_doesNotCreateAlert() {
        Ingredient atMinimum = ingredient(UUID.randomUUID(), "5.00", "5.00");
        when(ingredientRepository.findLowStock()).thenReturn(List.of());
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of());

        stockAlertService.syncAlertStates();

        verify(stockAlertRepository, never()).save(any(StockAlert.class));
        verify(ingredientRepository).findLowStock();
    }

    @Test
    void syncAlertStates_stockRecovered_resolvesOpenAlert() {
        Ingredient healthy = ingredient(UUID.randomUUID(), "10.00", "5.00");
        StockAlert open = alertFor(healthy, StockAlert.Status.OPEN);
        when(ingredientRepository.findLowStock()).thenReturn(List.of());
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of(open));

        stockAlertService.syncAlertStates();

        assertThat(open.getStatus()).isEqualTo(StockAlert.Status.RESOLVED);
        assertThat(open.getResolvedAt()).isNotNull();
        verify(stockAlertRepository).save(open);
    }

    @Test
    void syncAlertStates_stockFallsAgainAfterResolution_reopensAlert() {
        Ingredient low = ingredient(UUID.randomUUID(), "1.00", "5.00");
        StockAlert resolved = alertFor(low, StockAlert.Status.RESOLVED);
        resolved.setResolvedAt(Instant.now());
        when(ingredientRepository.findLowStock()).thenReturn(List.of(low));
        when(stockAlertRepository.findByIngredientIdIn(anyCollection())).thenReturn(List.of(resolved));
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of(resolved));

        stockAlertService.syncAlertStates();

        assertThat(resolved.getStatus()).isEqualTo(StockAlert.Status.OPEN);
        assertThat(resolved.getResolvedAt()).isNull();
        assertThat(resolved.getDetectedAt()).isNotNull();
        verify(stockAlertRepository).save(resolved);
    }

    @Test
    void syncAlertStates_notifiedStockChanged_reopensAlertForNewNotification() {
        Ingredient low = ingredient(UUID.randomUUID(), "3.00", "5.00");
        StockAlert notified = alertFor(low, StockAlert.Status.NOTIFIED);
        notified.setNotifiedAt(Instant.now());
        notified.setNotifiedCurrentStock(new BigDecimal("8.00"));
        notified.setNotifiedMinimumStock(new BigDecimal("5.00"));
        when(ingredientRepository.findLowStock()).thenReturn(List.of(low));
        when(stockAlertRepository.findByIngredientIdIn(anyCollection())).thenReturn(List.of(notified));
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of(notified));

        stockAlertService.syncAlertStates();

        assertThat(notified.getStatus()).isEqualTo(StockAlert.Status.OPEN);
        verify(stockAlertRepository).save(notified);
    }

    @Test
    void syncAlertStates_notifiedAlertUnchanged_doesNotReopen() {
        Ingredient low = ingredient(UUID.randomUUID(), "3.00", "5.00");
        StockAlert notified = alertFor(low, StockAlert.Status.NOTIFIED);
        notified.setNotifiedAt(Instant.now());
        notified.setNotifiedCurrentStock(new BigDecimal("3.00"));
        notified.setNotifiedMinimumStock(new BigDecimal("5.00"));
        when(ingredientRepository.findLowStock()).thenReturn(List.of(low));
        when(stockAlertRepository.findByIngredientIdIn(anyCollection())).thenReturn(List.of(notified));
        when(stockAlertRepository.findByStatusIn(anyCollection())).thenReturn(List.of(notified));

        stockAlertService.syncAlertStates();

        assertThat(notified.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
        verify(stockAlertRepository, never()).save(any(StockAlert.class));
    }

    @Test
    void handleIngredientRemoved_deletesAlertsAndDetachesNotificationItems() {
        UUID ingredientId = UUID.randomUUID();

        stockAlertService.handleIngredientRemoved(ingredientId);

        verify(stockNotificationItemRepository).detachByIngredientId(ingredientId);
        verify(stockAlertRepository).deleteByIngredientId(ingredientId);
    }

    @Test
    void getOverview_returnsAlertsAndRecentNotifications() {
        Ingredient low = ingredient(UUID.randomUUID(), "2.00", "5.00");
        StockAlert open = alertFor(low, StockAlert.Status.OPEN);
        when(stockAlertRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(open));
        when(stockNotificationRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());

        var overview = stockAlertService.getOverview();

        assertThat(overview.alerts()).hasSize(1);
        assertThat(overview.alerts().get(0).ingredientName()).isEqualTo(low.getName());
        assertThat(overview.alerts().get(0).status()).isEqualTo("OPEN");
        assertThat(overview.notifications()).isEmpty();
    }
}
