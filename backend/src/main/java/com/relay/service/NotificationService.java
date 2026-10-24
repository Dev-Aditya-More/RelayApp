package com.relay.service;

import com.relay.domain.Enums.Role;
import com.relay.domain.Notification;
import com.relay.domain.User;
import com.relay.repo.NotificationRepository;
import com.relay.repo.UserRepository;
import com.relay.web.Dtos.NotificationView;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final UserRepository users;

    public NotificationService(NotificationRepository repo, UserRepository users) {
        this.repo = repo;
        this.users = users;
    }

    /** Stores an in-app notification. Email/SMS delivery is simulated through the log. */
    public void notify(User user, String message, Long workOrderId) {
        if (user == null) return;
        repo.save(new Notification(user, message, workOrderId));
        log.info("[email/sms -> {}] {}", user.getEmail(), message);
    }

    public void notifyAdmins(String message, Long workOrderId) {
        users.findByRole(Role.ADMIN).forEach(a -> notify(a, message, workOrderId));
    }

    @Transactional(readOnly = true)
    public List<NotificationView> recent(User user) {
        return repo.findTop30ByUserOrderByCreatedAtDesc(user).stream()
                .map(n -> new NotificationView(n.getId(), n.getMessage(), n.getWorkOrderId(), n.isSeen(), n.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void markAllSeen(User user) {
        var unseen = repo.findByUserAndSeenFalse(user);
        unseen.forEach(n -> n.setSeen(true));
        repo.saveAll(unseen);
    }
}
