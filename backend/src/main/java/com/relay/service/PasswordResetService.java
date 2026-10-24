package com.relay.service;

import com.relay.domain.PasswordResetToken;
import com.relay.domain.User;
import com.relay.repo.PasswordResetTokenRepository;
import com.relay.repo.UserRepository;
import com.relay.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final MailService mail;
    private final String appUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
                                MailService mail, @Value("${relay.app-url}") String appUrl) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
        this.appUrl = appUrl.replaceAll("/+$", "");
    }

    /** Always returns quietly so the response never reveals whether the email has an account. */
    @Transactional
    public void request(String email) {
        User u = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (u == null || u.isSuspendedAccount()) return;
        var last = tokens.findFirstByUserOrderByCreatedAtDesc(u);
        if (last.isPresent() && last.get().getCreatedAt().plus(COOLDOWN).isAfter(Instant.now())) return;

        tokens.findByUserAndUsedAtIsNull(u).forEach(t -> t.setUsedAt(Instant.now()));
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new PasswordResetToken(u, hash(token), Instant.now().plus(LIFETIME)));

        String link = appUrl + "/reset-password?token=" + token;
        String to = u.getEmail(), name = u.getName();
        // Off the request thread so response time does not reveal whether the account exists.
        CompletableFuture.runAsync(() -> mail.sendPasswordReset(to, name, link));
    }

    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(hash(token))
                .filter(x -> x.getUsedAt() == null && x.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.bad("This reset link is invalid or has expired. Request a new one."));
        User u = t.getUser();
        if (u.isSuspendedAccount()) throw ApiException.forbidden("This account has been suspended. Contact Relay support.");
        u.setPasswordHash(encoder.encode(newPassword));
        u.setPasswordChangedAt(Instant.now());
        u.setEmailVerified(true);
        users.save(u);
        t.setUsedAt(Instant.now());
        tokens.findByUserAndUsedAtIsNull(u).forEach(x -> x.setUsedAt(Instant.now()));
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
