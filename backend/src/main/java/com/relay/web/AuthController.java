package com.relay.web;

import com.relay.domain.User;
import com.relay.service.AuthService;
import com.relay.service.Mapper;
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

    public AuthController(AuthService auth, Mapper mapper) {
        this.auth = auth;
        this.mapper = mapper;
    }

    @PostMapping("/auth/register")
    AuthRes register(@Valid @RequestBody RegisterReq req) { return auth.register(req); }

    @PostMapping("/auth/login")
    AuthRes login(@Valid @RequestBody LoginReq req) { return auth.login(req); }

    @GetMapping("/auth/me")
    UserView me(@AuthenticationPrincipal User user) { return mapper.view(user); }

    @GetMapping("/health")
    Map<String, String> health() { return Map.of("status", "ok"); }
}
