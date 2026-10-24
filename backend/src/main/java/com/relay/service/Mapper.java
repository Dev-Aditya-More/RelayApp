package com.relay.service;

import com.relay.domain.*;
import com.relay.repo.SlaPolicyRepository;
import com.relay.web.Dtos.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Entity -> DTO conversion. Call from inside a transaction: it touches lazy associations. */
@Component
public class Mapper {
    private final SlaPolicyRepository policies;
    private final PredictionService prediction;

    public Mapper(SlaPolicyRepository policies, PredictionService prediction) {
        this.policies = policies;
        this.prediction = prediction;
    }

    public UserView view(User u) {
        return u == null ? null : new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone());
    }

    public WorkOrderView view(WorkOrder w) {
        SlaPolicy p = policies.findByCategory(w.getCategory()).orElse(null);
        return new WorkOrderView(
                w.getId(), w.getCode(), w.getTitle(), w.getDescription(),
                w.getCategory(), w.getSeverity(), w.getStatus(),
                w.getArea(), w.getAddress(), w.getLatitude(), w.getLongitude(), w.getPhotoUrl(),
                w.getCitizen() == null ? null : w.getCitizen().getName(), view(w.getContractor()),
                p == null ? "General Services" : p.getDepartment(),
                w.isFastTrack(), w.isSlaBreached(), w.isEscalated(), w.isContractorPaid(), w.isFineIssued(),
                w.getCreatedAt(), w.getAssignedAt(), w.getStartedAt(), w.getCompletedAt(), w.getSlaDeadline(),
                p == null ? 0 : p.getHours(),
                p == null ? BigDecimal.ZERO : p.getFineAmount(),
                p == null ? BigDecimal.ZERO : p.getContractorRate(),
                p == null ? BigDecimal.ZERO : p.getFastTrackFee(),
                prediction.delayRisk(w), w.getRating(), w.getFeedback(), w.getSentiment());
    }

    public PaymentView view(Payment p) {
        WorkOrder w = p.getWorkOrder();
        return new PaymentView(p.getId(), p.getType(), p.getStatus(), p.getAmount(),
                p.getPayer() == null ? null : p.getPayer().getName(),
                p.getPayee() == null ? null : p.getPayee().getName(),
                w == null ? null : w.getId(), w == null ? null : w.getCode(), w == null ? null : w.getTitle(),
                p.getRazorpayOrderId(), p.getRazorpayPaymentId(), p.getInvoiceNo(), p.getNote(),
                p.getCreatedAt(), p.getPaidAt());
    }

    public EventView view(WorkOrderEvent e) {
        return new EventView(e.getActor(), e.getKind(), e.getMessage(), e.getPhotoUrl(), e.getCreatedAt());
    }

    public PolicyView view(SlaPolicy p) {
        return new PolicyView(p.getId(), p.getCategory(), p.getDepartment(), p.getHours(),
                p.getFineAmount(), p.getContractorRate(), p.getFastTrackFee());
    }
}
