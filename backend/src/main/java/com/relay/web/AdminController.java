package com.relay.web;

import com.relay.domain.Enums.Role;
import com.relay.domain.User;
import com.relay.repo.SlaPolicyRepository;
import com.relay.repo.UserRepository;
import com.relay.service.AnalyticsService;
import com.relay.service.Mapper;
import com.relay.service.NotificationService;
import com.relay.web.Dtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {
    private final AnalyticsService analytics;
    private final SlaPolicyRepository policies;
    private final UserRepository users;
    private final NotificationService notifications;
    private final Mapper mapper;

    public AdminController(AnalyticsService analytics, SlaPolicyRepository policies, UserRepository users,
                           NotificationService notifications, Mapper mapper) {
        this.analytics = analytics;
        this.policies = policies;
        this.users = users;
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/analytics")
    Map<String, Object> analytics() { return analytics.summary(); }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/contractors")
    List<UserView> contractors() { return users.findByRole(Role.CONTRACTOR).stream()
            .filter(u -> u.isApprovedAccount() && !u.isSuspendedAccount() && u.isEmailVerifiedAccount()).map(mapper::view).toList(); }

    @GetMapping("/sla-policies")
    List<PolicyView> policies() { return policies.findAll().stream().map(mapper::view).toList(); }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/sla-policies/{id}")
    PolicyView updatePolicy(@PathVariable Long id, @Valid @RequestBody PolicyReq req) {
        var p = policies.findById(id).orElseThrow(() -> ApiException.notFound("Policy"));
        p.setHours(req.hours());
        p.setFineAmount(req.fineAmount());
        p.setContractorRate(req.contractorRate());
        p.setFastTrackFee(req.fastTrackFee());
        return mapper.view(policies.save(p));
    }

    @GetMapping("/notifications")
    List<NotificationView> notifications(@AuthenticationPrincipal User user) { return notifications.recent(user); }

    @PostMapping("/notifications/read-all")
    Map<String, String> readAll(@AuthenticationPrincipal User user) {
        notifications.markAllSeen(user);
        return Map.of("status", "ok");
    }
}
