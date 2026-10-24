package com.relay.service;

import com.relay.domain.*;
import com.relay.domain.Enums.*;
import com.relay.repo.*;
import com.relay.web.ApiException;
import com.relay.web.Dtos.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class WorkOrderService {
    private final WorkOrderRepository orders;
    private final WorkOrderEventRepository events;
    private final SlaPolicyRepository policies;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final ClassifierService classifier;
    private final NotificationService notifications;
    private final FileStorage files;
    private final Mapper mapper;

    public WorkOrderService(WorkOrderRepository orders, WorkOrderEventRepository events, SlaPolicyRepository policies,
                            UserRepository users, PaymentRepository payments, ClassifierService classifier,
                            NotificationService notifications, FileStorage files, Mapper mapper) {
        this.orders = orders;
        this.events = events;
        this.policies = policies;
        this.users = users;
        this.payments = payments;
        this.classifier = classifier;
        this.notifications = notifications;
        this.files = files;
        this.mapper = mapper;
    }

    public WorkOrderView create(User citizen, String title, String description, Category category, Severity severity,
                                String area, String address, Double lat, Double lng, MultipartFile photo) {
        Suggestion hint = classifier.classify(title + " " + description);
        WorkOrder wo = new WorkOrder();
        wo.setTitle(title.trim());
        wo.setDescription(description);
        wo.setCategory(category != null ? category : hint.category());
        wo.setSeverity(severity != null ? severity : hint.severity());
        wo.setArea(area == null || area.isBlank() ? "Unspecified" : area.trim());
        wo.setAddress(address);
        wo.setLatitude(lat);
        wo.setLongitude(lng);
        wo.setPhotoUrl(files.saveImage(photo));
        wo.setCitizen(citizen);
        SlaPolicy policy = policies.findByCategory(wo.getCategory()).orElseThrow(() -> ApiException.bad("No SLA policy for category"));
        wo.setSlaDeadline(wo.getCreatedAt().plus(Duration.ofHours(policy.getHours())));
        orders.save(wo);
        wo.setCode(String.format("RLY-%06d", wo.getId()));
        events.save(new WorkOrderEvent(wo, citizen.getName(), "REPORTED", "Issue reported and work order " + wo.getCode() + " created", wo.getPhotoUrl()));
        notifications.notifyAdmins("New " + wo.getCategory().name().toLowerCase().replace('_', ' ') + " report in " + wo.getArea() + " (" + wo.getCode() + ")", wo.getId());
        notifications.notify(citizen, "Your report " + wo.getCode() + " is logged. SLA: " + policy.getHours() + "h", wo.getId());
        return mapper.view(wo);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderView> list(User user, Status status) {
        List<WorkOrder> all = switch (user.getRole()) {
            case ADMIN -> orders.findAllByOrderByCreatedAtDesc();
            case CONTRACTOR -> orders.findByContractorOrderByCreatedAtDesc(user);
            case CITIZEN -> orders.findByCitizenOrderByCreatedAtDesc(user);
        };
        return all.stream().filter(w -> status == null || w.getStatus() == status).map(mapper::view).toList();
    }

    @Transactional(readOnly = true)
    public WorkOrderDetail detail(User user, Long id) {
        WorkOrder wo = load(id);
        requireAccess(user, wo);
        return new WorkOrderDetail(mapper.view(wo),
                events.findByWorkOrderOrderByCreatedAtAsc(wo).stream().map(mapper::view).toList(),
                payments.findByWorkOrder(wo).stream().map(mapper::view).toList());
    }

    public WorkOrderView assign(User admin, Long id, Long contractorId) {
        WorkOrder wo = load(id);
        if (wo.getStatus() != Status.OPEN && wo.getStatus() != Status.ASSIGNED)
            throw ApiException.bad("Only open work orders can be (re)assigned");
        User c = users.findById(contractorId).filter(u -> u.getRole() == Role.CONTRACTOR)
                .orElseThrow(() -> ApiException.bad("Choose a valid contractor"));
        wo.setContractor(c);
        wo.setStatus(Status.ASSIGNED);
        wo.setAssignedAt(Instant.now());
        events.save(new WorkOrderEvent(wo, admin.getName(), "ASSIGNED", "Handed off to " + c.getName(), null));
        notifications.notify(c, "New work order " + wo.getCode() + " assigned to you", wo.getId());
        notifications.notify(wo.getCitizen(), wo.getCode() + " has been assigned to " + c.getName(), wo.getId());
        return mapper.view(wo);
    }

    public WorkOrderView updateStatus(User actor, Long id, Status target, String note, MultipartFile photo) {
        WorkOrder wo = load(id);
        String photoUrl = files.saveImage(photo);
        if (actor.getRole() == Role.CONTRACTOR) {
            if (wo.getContractor() == null || !wo.getContractor().getId().equals(actor.getId()))
                throw ApiException.forbidden("This work order is not assigned to you");
            boolean ok = (wo.getStatus() == Status.ASSIGNED && target == Status.IN_PROGRESS)
                    || (wo.getStatus() == Status.IN_PROGRESS && target == Status.COMPLETED);
            if (!ok) throw ApiException.bad("Cannot move from " + wo.getStatus() + " to " + target);
        } else if (actor.getRole() == Role.ADMIN) {
            if (target != Status.CLOSED || wo.getStatus() != Status.COMPLETED)
                throw ApiException.bad("Admins can close completed work orders");
        } else {
            throw ApiException.forbidden("Citizens cannot change status");
        }

        wo.setStatus(target);
        Instant now = Instant.now();
        String msg = (note == null || note.isBlank()) ? "Status changed to " + target.name().replace('_', ' ').toLowerCase() : note.trim();
        if (target == Status.IN_PROGRESS) {
            wo.setStartedAt(now);
            notifications.notify(wo.getCitizen(), "Work has started on " + wo.getCode(), wo.getId());
        } else if (target == Status.COMPLETED) {
            wo.setCompletedAt(now);
            if (now.isAfter(wo.getSlaDeadline())) wo.setSlaBreached(true);
            notifications.notify(wo.getCitizen(), wo.getCode() + " is resolved. Tell us how it went", wo.getId());
            notifications.notifyAdmins(wo.getCode() + " completed" + (wo.isSlaBreached() ? " (SLA breached)" : "") + ". Payout can be released", wo.getId());
        }
        events.save(new WorkOrderEvent(wo, actor.getName(), target.name(), msg, photoUrl));
        return mapper.view(wo);
    }

    public WorkOrderView rate(User citizen, Long id, int rating, String feedback) {
        WorkOrder wo = load(id);
        if (!wo.getCitizen().getId().equals(citizen.getId())) throw ApiException.forbidden("Not your report");
        if (wo.getStatus() != Status.COMPLETED) throw ApiException.bad("You can rate once the work is completed");
        wo.setRating(rating);
        wo.setFeedback(feedback);
        wo.setSentiment(classifier.sentiment(feedback, rating));
        wo.setStatus(Status.CLOSED);
        events.save(new WorkOrderEvent(wo, citizen.getName(), "CLOSED", "Rated " + rating + "/5" + (feedback == null || feedback.isBlank() ? "" : ": " + feedback), null));
        notifications.notify(wo.getContractor(), wo.getCode() + " was rated " + rating + "/5", wo.getId());
        return mapper.view(wo);
    }

    /** Called once a fast-track payment is captured: halve the remaining SLA window. */
    public void grantFastTrack(WorkOrder wo) {
        SlaPolicy p = policies.findByCategory(wo.getCategory()).orElseThrow();
        Instant fast = Instant.now().plus(Duration.ofMinutes(p.getHours() * 30L));
        if (fast.isBefore(wo.getSlaDeadline())) wo.setSlaDeadline(fast);
        wo.setFastTrack(true);
        wo.setSeverity(Severity.HIGH);
        events.save(new WorkOrderEvent(wo, "Relay", "FAST_TRACK", "Fast-track paid: priority handling, new SLA deadline set", null));
        notifications.notifyAdmins(wo.getCode() + " upgraded to fast-track", wo.getId());
        notifications.notify(wo.getContractor(), wo.getCode() + " is now fast-track: deadline moved up", wo.getId());
    }

    WorkOrder load(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Work order"));
    }

    void requireAccess(User user, WorkOrder wo) {
        boolean ok = switch (user.getRole()) {
            case ADMIN -> true;
            case CITIZEN -> wo.getCitizen().getId().equals(user.getId());
            case CONTRACTOR -> wo.getContractor() != null && wo.getContractor().getId().equals(user.getId());
        };
        if (!ok) throw ApiException.forbidden("You do not have access to this work order");
    }
}
