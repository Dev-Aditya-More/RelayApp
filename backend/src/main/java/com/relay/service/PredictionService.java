package com.relay.service;

import com.relay.domain.Enums.Severity;
import com.relay.domain.Enums.Status;
import com.relay.domain.SlaPolicy;
import com.relay.domain.WorkOrder;
import com.relay.repo.SlaPolicyRepository;
import com.relay.repo.WorkOrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import org.springframework.stereotype.Service;

/**
 * Delay-risk model (0-99). A transparent heuristic over SLA elapsed time, assignment state,
 * contractor workload and how long this category historically takes against its SLA.
 */
@Service
public class PredictionService {
    private static final EnumSet<Status> ACTIVE = EnumSet.of(Status.OPEN, Status.ASSIGNED, Status.IN_PROGRESS);

    private final WorkOrderRepository orders;
    private final SlaPolicyRepository policies;

    public PredictionService(WorkOrderRepository orders, SlaPolicyRepository policies) {
        this.orders = orders;
        this.policies = policies;
    }

    /** Returns null for work orders that are no longer active. */
    public Integer delayRisk(WorkOrder wo) {
        if (!ACTIVE.contains(wo.getStatus())) return null;
        if (wo.isSlaBreached()) return 99;

        Instant now = Instant.now();
        double total = Math.max(1, Duration.between(wo.getCreatedAt(), wo.getSlaDeadline()).toMinutes());
        double elapsed = Duration.between(wo.getCreatedAt(), now).toMinutes() / total;
        double score = Math.min(elapsed, 1.2) * 60;

        if (wo.getStatus() == Status.OPEN) score += 15;
        else if (wo.getStatus() == Status.ASSIGNED) score += 5;

        if (wo.getContractor() != null) {
            long load = orders.countByContractorAndStatusIn(wo.getContractor(), ACTIVE);
            score += Math.min(15, Math.max(0, (load - 1) * 5));
        }
        score += historyPenalty(wo);
        if (wo.getSeverity() == Severity.HIGH) score += 5;
        return (int) Math.max(1, Math.min(99, Math.round(score)));
    }

    /** Up to 20 points when finished jobs in this category usually run close to, or over, their SLA. */
    private double historyPenalty(WorkOrder wo) {
        SlaPolicy policy = policies.findByCategory(wo.getCategory()).orElse(null);
        if (policy == null) return 0;
        var done = orders.findAll().stream()
                .filter(o -> o.getCategory() == wo.getCategory() && o.getCompletedAt() != null)
                .toList();
        if (done.isEmpty()) return 0;
        double avgRatio = done.stream()
                .mapToDouble(o -> Duration.between(o.getCreatedAt(), o.getCompletedAt()).toMinutes() / (policy.getHours() * 60.0))
                .average().orElse(0);
        return Math.max(0, Math.min(20, (avgRatio - 0.6) * 40));
    }
}
