package com.relay.service;

import com.relay.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin Razorpay client: order creation over the REST API plus HMAC signature checks.
 * With no key configured it runs in mock mode so the whole flow works without real money.
 */
@Component
public class RazorpayGateway {
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final RestClient http = RestClient.create("https://api.razorpay.com/v1");

    public RazorpayGateway(@Value("${relay.razorpay.key-id}") String keyId,
                           @Value("${relay.razorpay.key-secret}") String keySecret,
                           @Value("${relay.razorpay.webhook-secret}") String webhookSecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    public boolean isMock() { return keyId == null || keyId.isBlank(); }

    public String keyId() { return isMock() ? "rzp_mock" : keyId; }

    @SuppressWarnings("unchecked")
    public String createOrder(long amountPaise, String receipt) {
        if (isMock()) return "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        try {
            Map<String, Object> res = http.post().uri("/orders")
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", amountPaise, "currency", "INR", "receipt", receipt))
                    .retrieve().body(Map.class);
            return String.valueOf(res.get("id"));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Razorpay order creation failed: " + e.getMessage());
        }
    }

    /** Checkout signature: HMAC_SHA256(order_id + "|" + payment_id, key_secret). */
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        return !isMock() && safeEquals(hmac(orderId + "|" + paymentId, keySecret), signature);
    }

    /** Webhook signature: HMAC_SHA256(raw body, webhook_secret) from the X-Razorpay-Signature header. */
    public boolean verifyWebhook(String body, String signature) {
        return webhookSecret != null && !webhookSecret.isBlank() && signature != null
                && safeEquals(hmac(body, webhookSecret), signature);
    }

    private static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
