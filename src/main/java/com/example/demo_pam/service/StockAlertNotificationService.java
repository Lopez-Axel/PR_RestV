package com.example.demo_pam.service;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.StockAlert;
import com.example.demo_pam.entity.StockNotification;
import com.example.demo_pam.entity.StockNotificationItem;
import com.example.demo_pam.entity.User;
import com.example.demo_pam.repository.StockAlertRepository;
import com.example.demo_pam.repository.StockNotificationItemRepository;
import com.example.demo_pam.repository.StockNotificationRepository;
import com.example.demo_pam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class StockAlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(StockAlertNotificationService.class);

    private final StockAlertService stockAlertService;
    private final StockAlertRepository stockAlertRepository;
    private final StockNotificationRepository stockNotificationRepository;
    private final StockNotificationItemRepository stockNotificationItemRepository;
    private final StockAlertEmailService stockAlertEmailService;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final boolean enabled;
    private final long retryIntervalMinutes;
    private final int maxAttempts;
    private final ReentrantLock evaluationLock = new ReentrantLock();

    public StockAlertNotificationService(StockAlertService stockAlertService,
                                         StockAlertRepository stockAlertRepository,
                                         StockNotificationRepository stockNotificationRepository,
                                         StockNotificationItemRepository stockNotificationItemRepository,
                                         StockAlertEmailService stockAlertEmailService,
                                         UserRepository userRepository,
                                         PlatformTransactionManager transactionManager,
                                         @Value("${app.stock-alerts.enabled:true}") boolean enabled,
                                         @Value("${app.stock-alerts.retry-interval-minutes:30}") long retryIntervalMinutes,
                                         @Value("${app.stock-alerts.max-attempts:3}") int maxAttempts) {
        this.stockAlertService = stockAlertService;
        this.stockAlertRepository = stockAlertRepository;
        this.stockNotificationRepository = stockNotificationRepository;
        this.stockNotificationItemRepository = stockNotificationItemRepository;
        this.stockAlertEmailService = stockAlertEmailService;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
        this.retryIntervalMinutes = retryIntervalMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.stock-alerts.check-delay-ms:3600000}",
            initialDelayString = "${app.stock-alerts.initial-delay-ms:60000}")
    public void scheduledCheck() {
        evaluate();
    }

    public void evaluate() {
        if (!enabled) {
            return;
        }
        // Exclusión mutua entre la tarea programada y cualquier otra invocación simultánea
        if (!evaluationLock.tryLock()) {
            log.debug("[STOCK-ALERT] Ya hay una evaluación en curso; se omite este ciclo");
            return;
        }
        try {
            stockAlertService.syncAlertStates();
            StockNotificationWork work = transactionTemplate.execute(status -> prepareWork());
            if (work == null) {
                return;
            }
            StockAlertEmailService.SendResult result =
                    stockAlertEmailService.send(work.notification(), work.items());
            transactionTemplate.executeWithoutResult(status -> recordResult(work, result));
        } finally {
            evaluationLock.unlock();
        }
    }

    private StockNotificationWork prepareWork() {
        Instant now = Instant.now();
        recoverInterruptedNotifications(now);

        List<StockNotification> retryable = stockNotificationRepository
                .findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        StockNotification.Status.FAILED, maxAttempts, now);
        if (!retryable.isEmpty()) {
            StockNotification notification = retryable.get(0);
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            return workOf(notification);
        }

        if (stockNotificationRepository.existsByStatusAndNextAttemptAtGreaterThan(
                StockNotification.Status.PENDING, now)) {
            log.debug("[STOCK-ALERT] Hay un envío en curso; se omite este ciclo");
            return null;
        }
        // Una falla con reintentos pendientes bloquea la creación de nuevas notificaciones hasta agotarlos
        if (stockNotificationRepository.existsByStatusAndAttemptCountLessThan(
                StockNotification.Status.FAILED, maxAttempts)) {
            log.debug("[STOCK-ALERT] Hay reintentos de una notificación fallida pendientes; se omite este ciclo");
            return null;
        }

        List<StockAlert> openAlerts = stockAlertRepository.findByStatus(StockAlert.Status.OPEN);
        if (openAlerts.isEmpty()) {
            return null;
        }
        List<String> adminEmails = userRepository.findByRole(User.Role.ADMIN).stream()
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
        if (adminEmails.isEmpty()) {
            log.error("[STOCK-ALERT] No hay usuarios con rol ADMIN para recibir la alerta; "
                    + "no se envía el correo de {} ingrediente(s) con stock bajo", openAlerts.size());
            return null;
        }
        if (!stockAlertEmailService.isAvailable()) {
            log.error("[STOCK-ALERT] SMTP no disponible; no se enviará la alerta de {} ingrediente(s) con stock bajo",
                    openAlerts.size());
            return null;
        }

        StockNotification notification = new StockNotification();
        notification.setStatus(StockNotification.Status.PENDING);
        notification.setRecipient(String.join(", ", adminEmails));
        notification.setAttemptCount(1);
        notification.setNextAttemptAt(now.plus(retryIntervalMinutes, ChronoUnit.MINUTES));
        notification = stockNotificationRepository.save(notification);

        List<StockNotificationItem> items = new ArrayList<>();
        for (StockAlert alert : openAlerts) {
            Ingredient ingredient = alert.getIngredient();
            StockNotificationItem item = new StockNotificationItem();
            item.setNotification(notification);
            item.setIngredient(ingredient);
            item.setIngredientName(ingredient.getName());
            item.setCurrentStock(ingredient.getCurrentStock());
            item.setMinimumStock(ingredient.getMinimumStock());
            item.setUnitAbbreviation(ingredient.getUnit().getAbbreviation());
            items.add(item);
        }
        stockNotificationItemRepository.saveAll(items);
        return new StockNotificationWork(notification, items);
    }

    private void recordResult(StockNotificationWork work, StockAlertEmailService.SendResult result) {
        Instant now = Instant.now();
        StockNotification notification = stockNotificationRepository.findById(work.notification().getId())
                .orElseThrow();
        if (result.success()) {
            notification.setStatus(StockNotification.Status.SENT);
            notification.setSentAt(now);
            notification.setErrorMessage(null);
            notification.setNextAttemptAt(null);
            stockNotificationRepository.save(notification);
            for (StockNotificationItem item : work.items()) {
                if (item.getIngredient() == null) {
                    continue;
                }
                stockAlertRepository.findByIngredientId(item.getIngredient().getId()).ifPresent(alert -> {
                    if (alert.getStatus() == StockAlert.Status.OPEN) {
                        alert.setStatus(StockAlert.Status.NOTIFIED);
                        alert.setNotifiedAt(now);
                        alert.setNotifiedCurrentStock(item.getCurrentStock());
                        alert.setNotifiedMinimumStock(item.getMinimumStock());
                        stockAlertRepository.save(alert);
                    }
                });
            }
        } else {
            notification.setStatus(StockNotification.Status.FAILED);
            notification.setErrorMessage(result.errorMessage());
            notification.setNextAttemptAt(notification.getAttemptCount() >= maxAttempts
                    ? null
                    : now.plus(retryIntervalMinutes, ChronoUnit.MINUTES));
            stockNotificationRepository.save(notification);
            log.error("[STOCK-ALERT] Notificación {} registrada como fallida (intento {}/{}): {}",
                    notification.getId(), notification.getAttemptCount(), maxAttempts, result.errorMessage());
        }
    }

    private void recoverInterruptedNotifications(Instant now) {
        for (StockNotification notification : stockNotificationRepository
                .findByStatusAndNextAttemptAtLessThanEqual(StockNotification.Status.PENDING, now)) {
            notification.setStatus(StockNotification.Status.FAILED);
            notification.setErrorMessage("Envío interrumpido: la aplicación se detuvo antes de confirmar el resultado");
            stockNotificationRepository.save(notification);
        }
    }

    private StockNotificationWork workOf(StockNotification notification) {
        return new StockNotificationWork(
                notification, stockNotificationItemRepository.findByNotificationId(notification.getId()));
    }

    private record StockNotificationWork(StockNotification notification, List<StockNotificationItem> items) {
    }
}
