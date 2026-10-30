package com.example.demo_pam.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;

// Provider simulado: permite probar el flujo de transferencia sin credenciales reales del banco
@Component("mockBankPaymentProvider")
@ConditionalOnProperty(name = "app.bank.enabled", havingValue = "false", matchIfMissing = true)
public class MockBankPaymentProvider implements BankPaymentProvider {

    private static final int QR_SIZE = 250;

    @Override
    public DynamicQr createDynamicQr(UUID paymentId, BigDecimal amount) {
        String transactionId = "MOCK-" + UUID.randomUUID();
        return new DynamicQr(generateQr("BANK/" + transactionId + "/" + amount + "/" + paymentId), transactionId);
    }

    @Override
    public void verifyCallback(String signature, String payload) {
        // El mock acepta cualquier callback; el provider real verificará con el secreto del banco
    }

    private String generateQr(String content) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el QR dinámico", e);
        }
    }
}