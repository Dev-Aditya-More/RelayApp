package com.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One-time password reset token. Only the SHA-256 hash is stored, never the token itself. */
@Entity
@Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) private User user;

    @Column(unique = true, nullable = false)
    private String tokenHash;

    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt = Instant.now();

    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
