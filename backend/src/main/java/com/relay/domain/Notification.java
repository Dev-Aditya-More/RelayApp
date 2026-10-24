package com.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) private User user;

    private String message;
    private Long workOrderId;
    private boolean seen;
    private Instant createdAt = Instant.now();

    public Notification(User user, String message, Long workOrderId) {
        this.user = user;
        this.message = message;
        this.workOrderId = workOrderId;
    }
}
