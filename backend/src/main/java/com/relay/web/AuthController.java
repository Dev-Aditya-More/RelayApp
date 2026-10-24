package com.relay.web;

import com.relay.domain.User;
import com.relay.service.AuthService;
import com.relay.service.EmailVerificationService;
import com.relay.service.Mapper;
import com.relay.service.PasswordResetService;
import com.relay.web.Dtos.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;
    private final Mapper mapper;
    private final PasswordResetService reset;
    private final EmailVerificationService verification;

    public AuthController(AuthService auth, Mapper mapper, PasswordResetService reset, EmailVerificationService verification) {
        this.auth = auth;
        this.mapper = mapper;
        this.reset = reset;
        this.verification = verification;
    }

    @PostMapping("/auth/register")
    AuthRes register(@Valid @RequestBody RegisterReq req) { return auth.register(req); }

    @PostMapping("/auth/login")
    AuthRes login(@Valid @RequestBody LoginReq req) { return auth.login(req); }

    @PostMapping("/auth/forgot-password")
    Map<String, String> forgot(@Valid @RequestBody ForgotReq req) {
        reset.request(req.email());
        return Map.of("message", "If an account exists for that email, a reset link is on its way.");
    }

    @PostMapping("/auth/reset-password")
    Map<String, String> reset(@Valid @RequestBody ResetReq req) {
        reset.reset(req.token(), req.password());
        return Map.of("message", "Password updated. You can sign in now.");
    }

    @PostMapping("/auth/verify-email")
    Map<String, String> verifyEmail(@Valid @RequestBody VerifyEmailReq req) {
        verification.verify(req.token());
        return Map.of("message", "Email verified.");
    }

    @PostMapping("/auth/resend-verification")
    Map<String, String> resendVerification(@AuthenticationPrincipal User user) {
        verification.send(user);
        return Map.of("message", "Verification email sent.");
    }

    @GetMapping("/auth/me")
    UserView me(@AuthenticationPrincipal User user) { return mapper.view(user); }

    @GetMapping("/health")
    Map<String, String> health() { return Map.of("status", "ok"); }
}
