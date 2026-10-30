package com.example.demo_pam.service;

import com.example.demo_pam.entity.StockNotification;
import com.example.demo_pam.entity.StockNotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class StockAlertEmailService {

    static final String SUBJECT = "Alerta de inventario: ingredientes con stock bajo";

    private static final Logger log = LoggerFactory.getLogger(StockAlertEmailService.class);
    private static final DateTimeFormatter EVALUATION_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public StockAlertEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                  @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public boolean isAvailable() {
        return mailSenderProvider.getIfAvailable() != null;
    }

    public SendResult send(StockNotification notification, List<StockNotificationItem> items) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("[STOCK-ALERT] SMTP no disponible (JavaMailSender no configurado); no se envía la notificación {}", notification.getId());
            return new SendResult(false, "SMTP no disponible");
        }
        String[] recipients = parseRecipients(notification.getRecipient());
        if (recipients.length == 0) {
            log.error("[STOCK-ALERT] Destinatario no configurado; no se envía la notificación {}", notification.getId());
            return new SendResult(false, "Destinatario no configurado");
        }
        try {
            List<StockNotificationItem> sorted = items.stream()
                    .sorted(Comparator.comparing(StockNotificationItem::getIngredientName))
                    .toList();
            Instant evaluatedAt = Instant.now();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setTo(recipients);
            helper.setSubject(SUBJECT);
            helper.setText(buildPlainText(sorted, evaluatedAt), buildHtml(sorted, evaluatedAt));
            mailSender.send(message);
            log.info("[STOCK-ALERT] Notificación {} enviada a {} destinatario(s) ({} ingrediente(s))",
                    notification.getId(), recipients.length, sorted.size());
            return new SendResult(true, null);
        } catch (Exception e) {
            log.error("[STOCK-ALERT] Falló el envío de la notificación {}: {}", notification.getId(), e.getMessage());
            return new SendResult(false, truncate(e.getMessage()));
        }
    }

    private String[] parseRecipients(String rawRecipients) {
        if (rawRecipients == null || rawRecipients.isBlank()) {
            return new String[0];
        }
        return java.util.Arrays.stream(rawRecipients.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toArray(String[]::new);
    }

    private String buildPlainText(List<StockNotificationItem> items, Instant evaluatedAt) {
        StringBuilder text = new StringBuilder();
        text.append(SUBJECT).append("\n\n");
        text.append("Se detectaron ").append(items.size())
                .append(" ingrediente(s) con stock por debajo del mínimo:\n");
        for (StockNotificationItem item : items) {
            text.append("- ").append(item.getIngredientName())
                    .append(" | stock actual: ").append(item.getCurrentStock().toPlainString())
                    .append(" | unidad: ").append(item.getUnitAbbreviation())
                    .append(" | stock mínimo: ").append(item.getMinimumStock().toPlainString())
                    .append('\n');
        }
        text.append("\nFecha y hora de la evaluación: ").append(EVALUATION_DATE.format(evaluatedAt)).append('\n');
        text.append("Revise las necesidades de compra de estos ingredientes.\n");
        return text.toString();
    }

    private String buildHtml(List<StockNotificationItem> items, Instant evaluatedAt) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>")
                .append(escape(SUBJECT)).append("</title></head><body>");
        html.append("<h1>").append(escape(SUBJECT)).append("</h1>");
        html.append("<p>Se detectaron <strong>").append(items.size())
                .append("</strong> ingrediente(s) con stock por debajo del m\u00ednimo:</p>");
        html.append("<table cellpadding=\"6\" cellspacing=\"0\" border=\"1\">");
        html.append("<caption>Ingredientes con stock bajo</caption>");
        html.append("<thead><tr>")
                .append("<th scope=\"col\">Ingrediente</th>")
                .append("<th scope=\"col\">Stock actual</th>")
                .append("<th scope=\"col\">Unidad</th>")
                .append("<th scope=\"col\">Stock m\u00ednimo</th>")
                .append("</tr></thead><tbody>");
        for (StockNotificationItem item : items) {
            html.append("<tr><td>").append(escape(item.getIngredientName()))
                    .append("</td><td>").append(escape(item.getCurrentStock().toPlainString()))
                    .append("</td><td>").append(escape(item.getUnitAbbreviation()))
                    .append("</td><td>").append(escape(item.getMinimumStock().toPlainString()))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        html.append("<p>Fecha y hora de la evaluaci\u00f3n: ")
                .append(escape(EVALUATION_DATE.format(evaluatedAt))).append("</p>");
        html.append("<p>Revise las necesidades de compra de estos ingredientes.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Error de env\u00edo sin detalle";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public record SendResult(boolean success, String errorMessage) {
    }
}
