package com.example.demo_pam.service;

import com.example.demo_pam.entity.Ingredient;
import com.example.demo_pam.entity.MeasurementUnit;
import com.example.demo_pam.entity.StockNotification;
import com.example.demo_pam.entity.StockNotificationItem;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAlertEmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    private StockAlertEmailService stockAlertEmailService;

    @BeforeEach
    void setUp() {
        stockAlertEmailService = new StockAlertEmailService(mailSenderProvider, "no-reply@test.com");
    }

    private StockNotification notification(String recipient) {
        StockNotification notification = new StockNotification();
        notification.setId(UUID.randomUUID());
        notification.setStatus(StockNotification.Status.PENDING);
        notification.setRecipient(recipient);
        notification.setAttemptCount(1);
        return notification;
    }

    private StockNotificationItem item(String name, String current, String minimum) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(UUID.randomUUID());
        ingredient.setName(name);
        ingredient.setCurrentStock(new BigDecimal(current));
        ingredient.setMinimumStock(new BigDecimal(minimum));
        MeasurementUnit unit = new MeasurementUnit();
        unit.setAbbreviation("kg");
        ingredient.setUnit(unit);
        StockNotificationItem item = new StockNotificationItem();
        item.setIngredient(ingredient);
        item.setIngredientName(name);
        item.setCurrentStock(new BigDecimal(current));
        item.setMinimumStock(new BigDecimal(minimum));
        item.setUnitAbbreviation("kg");
        return item;
    }

    private static void appendContent(Object content, StringBuilder sb) throws Exception {
        if (content instanceof String text) {
            sb.append(text);
        } else if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                appendContent(multipart.getBodyPart(i).getContent(), sb);
            }
        } else if (content != null) {
            sb.append(content);
        }
    }

    @Test
    void send_buildsSubjectTableAndEscapesHtmlForAllRecipients() throws Exception {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        List<StockNotificationItem> items = List.of(
                item("Harina", "2.00", "5.00"),
                item("<b>Rara</b>", "1.00", "3.00"));
        StockNotification notification = notification("admin1@test.com, admin2@test.com");

        StockAlertEmailService.SendResult result = stockAlertEmailService.send(notification, items);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo(StockAlertEmailService.SUBJECT);
        assertThat(sent.getAllRecipients()).hasSize(2);
        StringBuilder content = new StringBuilder();
        appendContent(sent.getContent(), content);
        String all = content.toString();
        assertThat(all).contains("Harina").contains("kg").contains("2.00").contains("5.00");
        assertThat(all).contains("2 ingrediente(s)");
        assertThat(all).contains("&lt;b&gt;Rara&lt;/b&gt;");
        assertThat(all).contains("Fecha y hora de la evaluación");
        assertThat(all).contains("Revise las necesidades de compra");
    }

    @Test
    void send_smtpFailure_returnsFailureWithoutThrowing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("boom")).when(mailSender).send(any(MimeMessage.class));

        StockAlertEmailService.SendResult result =
                stockAlertEmailService.send(notification("admin1@test.com"), List.of(item("Harina", "2.00", "5.00")));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("boom");
    }

    @Test
    void send_withoutMailSender_returnsFailure() {
        StockAlertEmailService.SendResult result =
                stockAlertEmailService.send(notification("admin1@test.com"), List.of(item("Harina", "2.00", "5.00")));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("SMTP");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_blankRecipient_returnsFailureWithoutSending() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        StockAlertEmailService.SendResult result =
                stockAlertEmailService.send(notification("  "), List.of(item("Harina", "2.00", "5.00")));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Destinatario");
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    void isAvailable_reflectsMailSenderPresence() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        assertThat(stockAlertEmailService.isAvailable()).isTrue();

        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        assertThat(stockAlertEmailService.isAvailable()).isFalse();
    }
}
