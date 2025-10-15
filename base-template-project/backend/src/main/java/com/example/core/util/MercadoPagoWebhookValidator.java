package com.example.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class MercadoPagoWebhookValidator {

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    public boolean isValid(String signatureHeader, String requestBody) {
        try {
            if (signatureHeader == null || signatureHeader.isEmpty()) {
                return false;
            }

            String[] parts = signatureHeader.split(",");
            String timestamp = parts[0].split("=")[1];
            String signature = parts[1].split("=")[1];

            String data = timestamp + "." + requestBody;
            String calculated = hmacSha256(data, webhookSecret);

            return calculated.equals(signature);

        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular HMAC-SHA256", e);
        }
    }
}
