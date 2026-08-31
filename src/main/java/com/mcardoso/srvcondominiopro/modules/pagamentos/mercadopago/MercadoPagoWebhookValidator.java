package com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Valida o header `x-signature` do webhook do Mercado Pago.
// Manifesto: "id:<data.id>;request-id:<x-request-id>;ts:<ts>;" com HMAC-SHA256 (hex) do webhookSecret.
// Segredo não configurado => validação desligada (só loga um aviso).
@Component
public class MercadoPagoWebhookValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookValidator.class);

    private final MercadoPagoProperties properties;

    public MercadoPagoWebhookValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String dataId, String requestId, String signatureHeader) {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("app.mercadopago.webhook-secret não configurado — assinatura do webhook não validada");
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook do Mercado Pago sem header x-signature");
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String chave = kv[0].trim();
            String valor = kv[1].trim();
            if ("ts".equals(chave)) {
                ts = valor;
            } else if ("v1".equals(chave)) {
                v1 = valor;
            }
        }
        if (ts == null || v1 == null) {
            log.warn("Header x-signature do Mercado Pago em formato inesperado: {}", signatureHeader);
            return false;
        }

        StringBuilder manifesto = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            manifesto.append("id:").append(dataId.toLowerCase()).append(";");
        }
        if (requestId != null && !requestId.isBlank()) {
            manifesto.append("request-id:").append(requestId).append(";");
        }
        manifesto.append("ts:").append(ts).append(";");

        String esperado = hmacSha256Hex(secret, manifesto.toString());
        boolean ok = MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            log.warn("Assinatura do webhook do Mercado Pago inválida");
        }
        return ok;
    }

    private static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Falha ao calcular HMAC do webhook", ex);
        }
    }
}
