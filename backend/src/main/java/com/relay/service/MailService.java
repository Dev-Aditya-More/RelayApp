package com.relay.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends email through Resend's HTTP API when RESEND_API_KEY is set.
 * Without a key (local dev) the message is written to the server log instead.
 */
@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final String apiKey;
    private final String from;
    private final RestClient http = RestClient.create("https://api.resend.com");

    public MailService(@Value("${relay.mail.resend-api-key}") String apiKey,
                       @Value("${relay.mail.from}") String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    /** Best effort: failures are logged, never thrown, so callers cannot leak whether an address exists. */
    public void send(String to, String subject, String html, String plainFallback) {
        if (apiKey.isBlank()) {
            log.info("[mail disabled] To: {} | {} | {}", to, subject, plainFallback);
            return;
        }
        try {
            http.post().uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of("from", from, "to", List.of(to), "subject", subject, "html", html))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.error("Could not send mail to {}: {}", to, e.getMessage());
        }
    }

    public void sendVerification(String to, String name, String link) {
        String html = "<p>Welcome to Relay, " + HtmlUtils.htmlEscape(name) + ".</p>"
                + "<p>Confirm your email address to start reporting and tracking fixes. This link works for 24 hours:</p>"
                + "<p><a href=\"" + link + "\">Verify my email</a></p>"
                + "<p>If you didn't create a Relay account, you can ignore this email.</p>";
        send(to, "Verify your email for Relay", html, link);
    }

    public void sendPasswordReset(String to, String name, String link) {
        String html = "<p>Hi " + HtmlUtils.htmlEscape(name) + ",</p>"
                + "<p>We got a request to reset your Relay password. This link works for 30 minutes and can be used once:</p>"
                + "<p><a href=\"" + link + "\">Reset your password</a></p>"
                + "<p>If you didn't ask for this, you can ignore this email. Your password stays the same.</p>";
        send(to, "Reset your Relay password", html, link);
    }
}
