package com.example.demo_pam.service;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.entity.StockAlert;
import com.example.demo_pam.entity.StockNotification;
import com.example.demo_pam.entity.StockNotificationItem;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.repository.StockAlertRepository;
import com.example.demo_pam.repository.StockNotificationItemRepository;
import com.example.demo_pam.repository.StockNotificationRepository;
import com.example.demo_pam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertNotificationServiceTest {

    @Mock
    private StockAlertService stockAlertService;
    @Mock
    private StockAlertRepository stockAlertRepository;
    @Mock
    private StockNotificationRepository stockNotificationRepository;
    @Mock
    private StockNotificationItemRepository stockNotificationItemRepository;
    @Mock
    private StockAlertEmailService stockAlertEmailService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private StockNotification savedNotification;

    private StockAlertNotificationService newService(boolean enabled) {
        return new StockAlertNotificationService(
                stockAlertService, stockAlertRepository, stockNotificationRepository,
                stockNotificationItemRepository, stockAlertEmailService, userRepository,
                transactionManager, enabled, 30, 3);
    }

    private StockAlertNotificationService service;

    @BeforeEach
    void setUp() {
        savedNotification = null;
        service = newService(true);
    }

    private Ingredient ingredient(String name, String current, String minimum) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(UUID.randomUUID());
        ingredient.setName(name);
        ingredient.setCurrentStock(new BigDecimal(current));
        ingredient.setMinimumStock(new BigDecimal(minimum));
        MeasurementUnit unit = new MeasurementUnit();
        unit.setAbbreviation("kg");
        ingredient.setUnit(unit);
        return ingredient;
    }

    private StockAlert openAlert(Ingredient ingredient) {
        StockAlert alert = new StockAlert();
        alert.setIngredient(ingredient);
        alert.setStatus(StockAlert.Status.OPEN);
        alert.setDetectedAt(Instant.now());
        return alert;
    }

    private User admin(String email) {
        User user = new User();
        user.setEmail(email);
        user.setRole(User.Role.ADMIN);
        return user;
    }

    private StockNotificationItem itemFor(StockNotification notification, Ingredient ingredient) {
        StockNotificationItem item = new StockNotificationItem();
        item.setNotification(notification);
        item.setIngredient(ingredient);
        item.setIngredientName(ingredient.getName());
        item.setCurrentStock(ingredient.getCurrentStock());
        item.setMinimumStock(ingredient.getMinimumStock());
        item.setUnitAbbreviation("kg");
        return item;
    }

    private void stubPreparePhase(List<StockAlert> openAlerts) {
        when(stockAlertRepository.findByStatus(StockAlert.Status.OPEN)).thenReturn(openAlerts);
        when(userRepository.findByRole(User.Role.ADMIN))
                .thenReturn(List.of(admin("admin1@test.com"), admin("admin2@test.com")));
        when(stockAlertEmailService.isAvailable()).thenReturn(true);
        when(stockNotificationRepository.save(any(StockNotification.class))).thenAnswer(inv -> {
            StockNotification notification = inv.getArgument(0);
            if (notification.getId() == null) {
                notification.setId(UUID.randomUUID());
                savedNotification = notification;
            }
            return notification;
        });
    }

    private void stubRecordSuccess(StockAlert... alerts) {
        when(stockNotificationRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(savedNotification));
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));
        for (StockAlert alert : alerts) {
            when(stockAlertRepository.findByIngredientId(alert.getIngredient().getId()))
                    .thenReturn(Optional.of(alert));
        }
    }

    @Test
    void evaluate_lowStockIngredients_groupsThemInSingleEmailAndMarksNotified() {
        StockAlert first = openAlert(ingredient("Harina", "2.00", "5.00"));
        StockAlert second = openAlert(ingredient("Azúcar", "1.00", "4.00"));
        StockAlert third = openAlert(ingredient("Sal", "0.50", "2.00"));
        stubPreparePhase(List.of(first, second, third));
        stubRecordSuccess(first, second, third);
        when(stockAlertEmailService.send(any(), any()))
                .thenReturn(new StockAlertEmailService.SendResult(true, null));

        service.evaluate();

        verify(stockAlertService).syncAlertStates();
        verify(stockAlertEmailService, times(1)).send(any(), argThat(items -> items.size() == 3));
        assertThat(savedNotification.getStatus()).isEqualTo(StockNotification.Status.SENT);
        assertThat(savedNotification.getRecipient()).contains("admin1@test.com").contains("admin2@test.com");
        assertThat(savedNotification.getSentAt()).isNotNull();
        assertThat(first.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
        assertThat(second.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
        assertThat(third.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
        assertThat(first.getNotifiedCurrentStock()).isEqualByComparingTo("2.00");
        assertThat(first.getNotifiedMinimumStock()).isEqualByComparingTo("5.00");
    }

    @Test
    void evaluate_noOpenAlerts_sendsNothing() {
        when(stockAlertRepository.findByStatus(StockAlert.Status.OPEN)).thenReturn(List.of());

        service.evaluate();

        verify(stockAlertService).syncAlertStates();
        verify(stockAlertEmailService, never()).send(any(), any());
        verify(stockNotificationRepository, never()).save(any(StockNotification.class));
    }

    @Test
    void evaluate_stateComesFromDatabase_newServiceInstanceDoesNotResend() {
        when(stockAlertRepository.findByStatus(StockAlert.Status.OPEN)).thenReturn(List.of());

        StockAlertNotificationService freshInstance = newService(true);
        freshInstance.evaluate();

        verify(stockAlertService).syncAlertStates();
        verify(stockAlertEmailService, never()).send(any(), any());
    }

    @Test
    void evaluate_smtpFailure_marksNotificationFailedAndKeepsAlertOpen() {
        stubPreparePhase(List.of(openAlert(ingredient("Harina", "2.00", "5.00"))));
        when(stockNotificationRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(savedNotification));
        when(stockAlertEmailService.send(any(), any()))
                .thenReturn(new StockAlertEmailService.SendResult(false, "boom"));

        service.evaluate();

        assertThat(savedNotification.getStatus()).isEqualTo(StockNotification.Status.FAILED);
        assertThat(savedNotification.getErrorMessage()).isEqualTo("boom");
        assertThat(savedNotification.getNextAttemptAt()).isNotNull();
        assertThat(savedNotification.getSentAt()).isNull();
        verify(stockAlertRepository, never()).save(any(StockAlert.class));
    }

    @Test
    void evaluate_failedNotificationRetriedWhenDue_sendsAndMarksNotified() {
        StockNotification failed = new StockNotification();
        failed.setId(UUID.randomUUID());
        failed.setStatus(StockNotification.Status.FAILED);
        failed.setAttemptCount(1);
        failed.setRecipient("admin1@test.com");
        failed.setNextAttemptAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        Ingredient low = ingredient("Harina", "2.00", "5.00");
        StockAlert alert = openAlert(low);
        StockNotificationItem item = itemFor(failed, low);
        when(stockNotificationRepository.findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(StockNotification.Status.FAILED), eq(3), any(Instant.class)))
                .thenReturn(List.of(failed));
        when(stockNotificationItemRepository.findByNotificationId(failed.getId())).thenReturn(List.of(item));
        when(stockAlertEmailService.send(any(), any()))
                .thenReturn(new StockAlertEmailService.SendResult(true, null));
        when(stockNotificationRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
        when(stockAlertRepository.findByIngredientId(low.getId())).thenReturn(Optional.of(alert));
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        service.evaluate();

        verify(stockAlertEmailService, times(1)).send(any(), argThat(items -> items.size() == 1));
        assertThat(failed.getAttemptCount()).isEqualTo(2);
        assertThat(failed.getStatus()).isEqualTo(StockNotification.Status.SENT);
        assertThat(failed.getSentAt()).isNotNull();
        assertThat(alert.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
    }

    @Test
    void evaluate_interruptedPendingNotification_recoveredAndRetried() {
        StockNotification stuck = new StockNotification();
        stuck.setId(UUID.randomUUID());
        stuck.setStatus(StockNotification.Status.PENDING);
        stuck.setAttemptCount(1);
        stuck.setRecipient("admin1@test.com");
        stuck.setNextAttemptAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        Ingredient low = ingredient("Harina", "2.00", "5.00");
        StockAlert alert = openAlert(low);
        StockNotificationItem item = itemFor(stuck, low);
        when(stockNotificationRepository.findByStatusAndNextAttemptAtLessThanEqual(
                eq(StockNotification.Status.PENDING), any(Instant.class)))
                .thenReturn(List.of(stuck));
        when(stockNotificationRepository.findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(StockNotification.Status.FAILED), eq(3), any(Instant.class)))
                .thenReturn(List.of(stuck));
        when(stockNotificationItemRepository.findByNotificationId(stuck.getId())).thenReturn(List.of(item));
        when(stockAlertEmailService.send(any(), any()))
                .thenReturn(new StockAlertEmailService.SendResult(true, null));
        when(stockNotificationRepository.findById(stuck.getId())).thenReturn(Optional.of(stuck));
        when(stockAlertRepository.findByIngredientId(low.getId())).thenReturn(Optional.of(alert));
        when(stockAlertRepository.save(any(StockAlert.class))).thenAnswer(inv -> inv.getArgument(0));

        service.evaluate();

        verify(stockNotificationRepository, times(2)).save(stuck);
        assertThat(stuck.getStatus()).isEqualTo(StockNotification.Status.SENT);
        assertThat(stuck.getAttemptCount()).isEqualTo(2);
        assertThat(stuck.getErrorMessage()).isNull();
        assertThat(alert.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
    }

    @Test
    void evaluate_failedRetryNotDueYet_doesNotCreateNewNotification() {
        when(stockNotificationRepository.existsByStatusAndAttemptCountLessThan(
                eq(StockNotification.Status.FAILED), anyInt())).thenReturn(true);

        service.evaluate();

        verify(stockAlertRepository, never()).findByStatus(any(StockAlert.Status.class));
        verify(stockAlertEmailService, never()).send(any(), any());
        verify(stockNotificationRepository, never()).save(any(StockNotification.class));
    }

    @Test
    void evaluate_notificationInFlight_skipsCycle() {
        when(stockNotificationRepository.existsByStatusAndNextAttemptAtGreaterThan(
                eq(StockNotification.Status.PENDING), any(Instant.class))).thenReturn(true);

        service.evaluate();

        verify(stockAlertService).syncAlertStates();
        verify(stockAlertEmailService, never()).send(any(), any());
        verify(stockNotificationRepository, never()).save(any(StockNotification.class));
    }

    @Test
    void evaluate_withoutAdminUsers_doesNotSend() {
        when(stockAlertRepository.findByStatus(StockAlert.Status.OPEN))
                .thenReturn(List.of(openAlert(ingredient("Harina", "2.00", "5.00"))));
        when(userRepository.findByRole(User.Role.ADMIN)).thenReturn(List.of());

        service.evaluate();

        verify(stockAlertEmailService, never()).send(any(), any());
        verify(stockNotificationRepository, never()).save(any(StockNotification.class));
    }

    @Test
    void evaluate_disabled_isNoOp() {
        StockAlertNotificationService disabled = newService(false);

        disabled.evaluate();

        verifyNoInteractions(stockAlertService, stockAlertEmailService, stockNotificationRepository);
    }

    @Test
    void evaluate_exhaustedFailuresDoNotBlock_newNotificationWaveIsCreated() {
        StockAlert alert = openAlert(ingredient("Harina", "2.00", "5.00"));
        stubPreparePhase(List.of(alert));
        stubRecordSuccess(alert);
        when(stockNotificationRepository.existsByStatusAndAttemptCountLessThan(
                eq(StockNotification.Status.FAILED), anyInt())).thenReturn(false);
        when(stockAlertEmailService.send(any(), any()))
                .thenReturn(new StockAlertEmailService.SendResult(true, null));

        service.evaluate();

        ArgumentCaptor<StockNotification> captor = ArgumentCaptor.forClass(StockNotification.class);
        verify(stockNotificationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
        assertThat(savedNotification.getStatus()).isEqualTo(StockNotification.Status.SENT);
        assertThat(alert.getStatus()).isEqualTo(StockAlert.Status.NOTIFIED);
    }
}
