package com.relay.domain;

import com.relay.domain.Enums.*;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class WorkOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-friendly id such as RLY-000042, derived from the row id after first save. */
    private String code;

    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING) private Category category;
    @Enumerated(EnumType.STRING) private Severity severity;
    @Enumerated(EnumType.STRING) private Status status = Status.OPEN;

    private String area;
    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY) private User citizen;
    @ManyToOne(fetch = FetchType.LAZY) private User contractor;

    private boolean fastTrack;
    private boolean slaBreached;
    private boolean escalated;
    private boolean contractorPaid;
    private boolean fineIssued;

    private Instant createdAt = Instant.now();
    private Instant assignedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant slaDeadline;

    private Integer rating;
    @Column(length = 1000) private String feedback;
    private String sentiment;
}
