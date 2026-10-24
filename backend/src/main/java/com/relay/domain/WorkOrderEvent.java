package com.relay.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One hand-off in a work order's timeline; doubles as the audit log. */
@Entity
@Getter @Setter @NoArgsConstructor
public class WorkOrderEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) private WorkOrder workOrder;

    private String actor;
    private String kind;

    @Column(length = 1000) private String message;
    private String photoUrl;
    private Instant createdAt = Instant.now();

    public WorkOrderEvent(WorkOrder wo, String actor, String kind, String message, String photoUrl) {
        this.workOrder = wo;
        this.actor = actor;
        this.kind = kind;
        this.message = message;
        this.photoUrl = photoUrl;
    }
}
