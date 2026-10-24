package com.relay.service;

import com.relay.config.JwtService;
import com.relay.domain.Enums.Role;
import com.relay.domain.User;
import com.relay.repo.UserRepository;
import com.relay.web.ApiException;
import com.relay.web.Dtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, Mapper mapper) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
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
        users.save(u);
        return new AuthRes(jwt.issue(u), mapper.view(u));
    }

    public AuthRes login(LoginReq req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password"));
        return new AuthRes(jwt.issue(u), mapper.view(u));
    }
}
