package com.relay.service;

import com.relay.config.JwtService;
import com.relay.config.RateLimiter;
import com.relay.domain.Enums.Role;
import com.relay.domain.User;
import com.relay.repo.UserRepository;
import com.relay.web.ApiException;
import com.relay.web.Dtos.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final int MAX_FAILED_LOGINS = 5;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;
    private final EmailVerificationService verification;
    private final RateLimiter limiter;
    private final boolean requireVerification;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, Mapper mapper,
                       EmailVerificationService verification, RateLimiter limiter,
                       @Value("${relay.require-email-verification}") boolean requireVerification) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
        this.verification = verification;
        this.limiter = limiter;
        this.requireVerification = requireVerification;
    }

    public AuthRes register(RegisterReq req) {
        if (users.findByEmailIgnoreCase(req.email()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        // Admin accounts are never self-service.
        Role role = req.role() == Role.CONTRACTOR ? Role.CONTRACTOR : Role.CITIZEN;
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setPhone(req.phone());
        u.setRole(role);
        u.setApproved(role != Role.CONTRACTOR);
        u.setEmailVerified(!requireVerification);
        users.save(u);
        verification.send(u);
        return new AuthRes(jwt.issue(u), mapper.view(u));
    }

    public AuthRes login(LoginReq req) {
        // Lock an email out after repeated failures, whichever IP they come from.
        String key = "login-fail|" + req.email().trim().toLowerCase();
        long wait = limiter.blockedFor(key, MAX_FAILED_LOGINS);
        if (wait > 0)
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed sign-ins. Try again in " + RateLimiter.humanWait(wait) + " or reset your password.");
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> {
                    limiter.hit(key, MAX_FAILED_LOGINS, Duration.ofMinutes(15));
                    return new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
                });
        limiter.reset(key);
        if (u.isSuspendedAccount()) throw ApiException.forbidden("This account has been suspended. Contact Relay support.");
        return new AuthRes(jwt.issue(u), mapper.view(u));
    }
}
