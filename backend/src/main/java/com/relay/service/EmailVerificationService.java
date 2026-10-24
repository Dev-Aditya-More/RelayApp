package com.relay.service;

import com.relay.domain.EmailVerificationToken;
import com.relay.domain.User;
import com.relay.repo.EmailVerificationTokenRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final Duration LIFETIME = Duration.ofHours(24);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository users;
    private final EmailVerificationTokenRepository tokens;
    private final MailService mail;
    private final String appUrl;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(UserRepository users, EmailVerificationTokenRepository tokens,
                                    MailService mail, @Value("${relay.app-url}") String appUrl) {
        this.users = users;
        this.tokens = tokens;
        this.mail = mail;
        this.appUrl = appUrl.replaceAll("/+$", "");
    }

    /** Emails a fresh verification link. A user asking again within the cooldown gets a 429. */
    @Transactional
    public void send(User u) {
        if (u.isEmailVerifiedAccount()) return;
        var last = tokens.findFirstByUserOrderByCreatedAtDesc(u);
        if (last.isPresent() && last.get().getCreatedAt().plus(COOLDOWN).isAfter(Instant.now()))
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "We just sent a link. Please wait a minute before asking for another.");

        tokens.findByUserAndUsedAtIsNull(u).forEach(t -> t.setUsedAt(Instant.now()));
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new EmailVerificationToken(u, hash(token), Instant.now().plus(LIFETIME)));

        String link = appUrl + "/verify-email?token=" + token;
        String to = u.getEmail(), name = u.getName();
        CompletableFuture.runAsync(() -> mail.sendVerification(to, name, link));
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken t = tokens.findByTokenHash(hash(token))
                .filter(x -> x.getUsedAt() == null && x.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.bad("This verification link is invalid or has expired. Sign in and request a new one."));
        User u = t.getUser();
        u.setEmailVerified(true);
        users.save(u);
        t.setUsedAt(Instant.now());
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
