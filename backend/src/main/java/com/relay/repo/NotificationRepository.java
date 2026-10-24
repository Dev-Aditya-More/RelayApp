package com.relay.repo;

import com.relay.domain.Notification;
import com.relay.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop30ByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndSeenFalse(User user);
}
