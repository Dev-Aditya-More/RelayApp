package com.relay.service;

import com.relay.domain.Enums.Severity;
import com.relay.domain.Enums.Status;
import com.relay.domain.WorkOrder;
import com.relay.domain.WorkOrderEvent;
import com.relay.repo.WorkOrderEventRepository;
import com.relay.repo.WorkOrderRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Sweeps active work orders every minute and flags the ones that blew through their SLA. */
@Component
public class SlaScheduler {
    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);

    private final WorkOrderRepository orders;
    private final WorkOrderEventRepository events;
    private final NotificationService notifications;

    public SlaScheduler(WorkOrderRepository orders, WorkOrderEventRepository events, NotificationService notifications) {
        this.orders = orders;
        this.events = events;
        this.notifications = notifications;
    }

    @Scheduled(initialDelay = 20_000, fixedDelay = 60_000)
    @Transactional
    public void checkSla() {
        List<WorkOrder> overdue = orders.findByStatusInAndSlaBreachedFalseAndSlaDeadlineBefore(
                List.of(Status.OPEN, Status.ASSIGNED, Status.IN_PROGRESS), Instant.now());
        for (WorkOrder wo : overdue) {
            wo.setSlaBreached(true);
            String msg = "SLA breached";
            if (wo.getStatus() == Status.OPEN) {
                // nobody picked it up: escalate so it jumps the admin queue
                wo.setEscalated(true);
                wo.setSeverity(Severity.HIGH);
                msg += " while unassigned: escalated to high priority";
            }
            events.save(new WorkOrderEvent(wo, "Relay SLA Monitor", "SLA_BREACH", msg, null));
            notifications.notifyAdmins(wo.getCode() + ": " + msg, wo.getId());
            notifications.notify(wo.getContractor(), wo.getCode() + " has breached its SLA", wo.getId());
            notifications.notify(wo.getCitizen(), "We missed the target on " + wo.getCode() + ". It has been escalated", wo.getId());
        }
        if (!overdue.isEmpty()) log.info("SLA sweep flagged {} work order(s)", overdue.size());
    }
}
