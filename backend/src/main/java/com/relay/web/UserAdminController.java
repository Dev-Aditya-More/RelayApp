package com.relay.web;

import com.relay.domain.Enums.Role;
import com.relay.domain.User;
import com.relay.repo.UserRepository;
import com.relay.service.Mapper;
import com.relay.web.Dtos.*;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Admin-only account management: approve contractors, suspend accounts, grant admin, list demo logins. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserRepository users;
    private final Mapper mapper;
    private final String demoPassword;

    public UserAdminController(UserRepository users, Mapper mapper, @Value("${relay.demo-password}") String demoPassword) {
        this.users = users;
        this.mapper = mapper;
        this.demoPassword = demoPassword;
    }

    @GetMapping("/users")
    List<UserView> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(mapper::view).toList();
    }

    @PutMapping("/users/{id}")
    @Transactional
    UserView update(@AuthenticationPrincipal User admin, @PathVariable Long id, @RequestBody UserUpdateReq req) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        if (u.getId().equals(admin.getId()))
            throw ApiException.bad("You cannot change your own role or status");
        if (req.role() != null) {
            u.setRole(req.role());
            if (req.role() != Role.CONTRACTOR) u.setApproved(true);
        }
        if (Boolean.TRUE.equals(req.approved()) && !u.isEmailVerifiedAccount())
            throw ApiException.bad(u.getName() + " has not verified their email yet");
        if (req.approved() != null) u.setApproved(req.approved());
        if (req.suspended() != null) u.setSuspended(req.suspended());
        return mapper.view(users.save(u));
    }

    /** Demo logins for showing Relay to prospects. Never shown on the public login page. */
    @GetMapping("/demo-accounts")
    List<DemoAccount> demoAccounts() {
        return users.findByDemoTrue().stream()
                .map(u -> new DemoAccount(u.getName(), u.getEmail(), u.getRole(), demoPassword)).toList();
    }
}
