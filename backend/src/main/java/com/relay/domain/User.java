package com.relay.domain;

import com.relay.domain.Enums.Role;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String phone;

    /** Contractors start unapproved and cannot receive work until an admin approves them. Null (legacy rows) means approved. */
    private Boolean approved;
    /** Suspended accounts cannot sign in. */
    private Boolean suspended;
    /** Seeded demo account, listed to admins only. */
    private Boolean demo;
    /** Null (legacy rows) counts as verified. */
    private Boolean emailVerified;
    private Instant createdAt = Instant.now();
    /** Tokens issued before this moment are rejected (set on password reset). */
    private Instant passwordChangedAt;

    public boolean isApprovedAccount() { return approved == null || approved; }
    public boolean isSuspendedAccount() { return suspended != null && suspended; }
    public boolean isEmailVerifiedAccount() { return emailVerified == null || emailVerified; }
    public boolean isDemoAccount() { return demo != null && demo; }
}
