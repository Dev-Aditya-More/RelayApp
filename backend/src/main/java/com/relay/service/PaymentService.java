package com.relay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relay.domain.*;
import com.relay.domain.Enums.*;
import com.relay.repo.*;
import com.relay.web.ApiException;
import com.relay.web.Dtos.*;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository payments;
    private final WorkOrderRepository orders;
    private final WorkOrderEventRepository events;
    private final SlaPolicyRepository policies;
    private final WorkOrderService workOrders;
    private final NotificationService notifications;
    private final RazorpayGateway gateway;
    private final Mapper mapper;
    private final ObjectMapper json;

    public PaymentService(PaymentRepository payments, WorkOrderRepository orders, WorkOrderEventRepository events,
                          SlaPolicyRepository policies, WorkOrderService workOrders, NotificationService notifications,
                          RazorpayGateway gateway, Mapper mapper, ObjectMapper json) {
        this.payments = payments;
        this.orders = orders;
        this.events = events;
        this.policies = policies;
        this.workOrders = workOrders;
        this.notifications = notifications;
        this.gateway = gateway;
        this.mapper = mapper;
        this.json = json;
    }

    public boolean isMock() { return gateway.isMock(); }

    /** Citizen pays for priority handling; returns a checkout ready for Razorpay. */
    public CheckoutRes startFastTrack(User citizen, Long workOrderId) {
        WorkOrder wo = workOrders.load(workOrderId);
        if (!wo.getCitizen().getId().equals(citizen.getId())) throw ApiException.forbidden("Not your report");
        if (wo.isFastTrack()) throw ApiException.bad("This report is already fast-tracked");
        if (wo.getStatus() == Status.COMPLETED || wo.getStatus() == Status.CLOSED)
            throw ApiException.bad("This work order is already resolved");
        Payment p = payments.findByWorkOrder(wo).stream()
                .filter(x -> x.getType() == PaymentType.FAST_TRACK && x.getStatus() != PaymentStatus.PAID)
                .findFirst().orElseGet(() -> {
                    Payment n = new Payment();
                    n.setType(PaymentType.FAST_TRACK);
                    n.setPayer(citizen);
                    n.setWorkOrder(wo);
                    n.setAmount(policies.findByCategory(wo.getCategory()).orElseThrow().getFastTrackFee());
                    n.setNote("Premium fast-track for " + wo.getCode());
                    return payments.save(n);
                });
        return openCheckout(p);
    }

    /** Admin raises a penalty against the contractor of a breached work order. */
    public PaymentView issueFine(User admin, Long workOrderId) {
        WorkOrder wo = workOrders.load(workOrderId);
        if (!wo.isSlaBreached()) throw ApiException.bad("Fines apply only to SLA-breached work orders");
        if (wo.getContractor() == null) throw ApiException.bad("No contractor assigned");
        if (wo.isFineIssued()) throw ApiException.bad("A fine was already issued for this work order");
        Payment p = new Payment();
        p.setType(PaymentType.FINE);
        p.setPayer(wo.getContractor());
        p.setPayee(admin);
        p.setWorkOrder(wo);
        p.setAmount(policies.findByCategory(wo.getCategory()).orElseThrow().getFineAmount());
        p.setNote("SLA breach penalty for " + wo.getCode());
        payments.save(p);
        wo.setFineIssued(true);
        events.save(new WorkOrderEvent(wo, admin.getName(), "FINE", "SLA penalty of Rs " + p.getAmount() + " issued to " + wo.getContractor().getName(), null));
        notifications.notify(wo.getContractor(), "Penalty of Rs " + p.getAmount() + " issued for " + wo.getCode(), wo.getId());
        return mapper.view(p);
    }

    /** Admin releases the contractor's payment once the work is completed. */
    public PaymentView releasePayout(User admin, Long workOrderId) {
        WorkOrder wo = workOrders.load(workOrderId);
        if (wo.getStatus() != Status.COMPLETED && wo.getStatus() != Status.CLOSED)
            throw ApiException.bad("Payment is released after the work is completed");
        if (wo.getContractor() == null) throw ApiException.bad("No contractor assigned");
        if (wo.isContractorPaid()) throw ApiException.bad("Payment already released");
        Payment p = new Payment();
        p.setType(PaymentType.CONTRACTOR_PAYOUT);
        p.setPayer(admin);
        p.setPayee(wo.getContractor());
        p.setWorkOrder(wo);
        p.setAmount(policies.findByCategory(wo.getCategory()).orElseThrow().getContractorRate());
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(Instant.now());
        p.setRazorpayPaymentId("payout_sim_" + UUID.randomUUID().toString().substring(0, 12));
        p.setNote("Simulated payout (settlement via RazorpayX is not wired in this MVP)");
        payments.save(p);
        p.setInvoiceNo(invoiceNo(p));
        wo.setContractorPaid(true);
        events.save(new WorkOrderEvent(wo, admin.getName(), "PAYOUT", "Contractor payment of Rs " + p.getAmount() + " released", null));
        notifications.notify(wo.getContractor(), "Payment of Rs " + p.getAmount() + " released for " + wo.getCode(), wo.getId());
        return mapper.view(p);
    }

    /** Opens (or reuses) a gateway order for a payment the current user owes. */
    public CheckoutRes checkout(User user, Long paymentId) {
        Payment p = payments.findById(paymentId).orElseThrow(() -> ApiException.notFound("Payment"));
        if (p.getPayer() == null || !p.getPayer().getId().equals(user.getId())) throw ApiException.forbidden("This payment is not yours");
        return openCheckout(p);
    }

    public PaymentView verify(User user, VerifyReq req) {
        Payment p = payments.findByRazorpayOrderId(req.razorpayOrderId()).orElseThrow(() -> ApiException.notFound("Payment"));
        if (!p.getPayer().getId().equals(user.getId())) throw ApiException.forbidden("This payment is not yours");
        if (!gateway.verifyPayment(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature()))
            throw ApiException.bad("Payment signature could not be verified");
        markPaid(p, req.razorpayPaymentId());
        return mapper.view(p);
    }

    /** Stand-in for Razorpay Checkout when no API keys are configured. */
    public PaymentView mockPay(User user, Long paymentId) {
        if (!gateway.isMock()) throw ApiException.bad("Mock payments are disabled when Razorpay keys are set");
        Payment p = payments.findById(paymentId).orElseThrow(() -> ApiException.notFound("Payment"));
        if (!p.getPayer().getId().equals(user.getId())) throw ApiException.forbidden("This payment is not yours");
        markPaid(p, "pay_mock_" + UUID.randomUUID().toString().substring(0, 12));
        return mapper.view(p);
    }

    /** Razorpay webhook: the server-side source of truth, safe to receive more than once. */
    public void handleWebhook(String body, String signature) {
        if (!gateway.verifyWebhook(body, signature)) throw ApiException.bad("Invalid webhook signature");
        try {
            JsonNode root = json.readTree(body);
            String event = root.path("event").asText();
            JsonNode entity = root.path("payload").path("payment").path("entity");
            String orderId = entity.path("order_id").asText(null);
            if (orderId == null) return;
            Payment p = payments.findByRazorpayOrderId(orderId).orElse(null);
            if (p == null) return;
            if (event.equals("payment.captured") || event.equals("order.paid")) {
                markPaid(p, entity.path("id").asText(null));
            } else if (event.equals("payment.failed") && p.getStatus() != PaymentStatus.PAID) {
                p.setStatus(PaymentStatus.FAILED);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.bad("Malformed webhook payload");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> list(User user) {
        List<Payment> all = user.getRole() == Role.ADMIN
                ? payments.findAllByOrderByCreatedAtDesc()
                : payments.findByPayerOrPayeeOrderByCreatedAtDesc(user, user);
        return all.stream().map(mapper::view).toList();
    }

    @Transactional(readOnly = true)
    public PaymentView invoice(User user, Long id) {
        Payment p = payments.findById(id).orElseThrow(() -> ApiException.notFound("Payment"));
        boolean mine = user.getRole() == Role.ADMIN
                || (p.getPayer() != null && p.getPayer().getId().equals(user.getId()))
                || (p.getPayee() != null && p.getPayee().getId().equals(user.getId()));
        if (!mine) throw ApiException.forbidden("Not your invoice");
        if (p.getStatus() != PaymentStatus.PAID) throw ApiException.bad("Invoice is available once paid");
        return mapper.view(p);
    }

    private CheckoutRes openCheckout(Payment p) {
        if (p.getStatus() == PaymentStatus.PAID) throw ApiException.bad("Already paid");
        if (p.getRazorpayOrderId() == null) {
            p.setRazorpayOrderId(gateway.createOrder(p.getAmount().movePointRight(2).longValueExact(), "relay_" + p.getId()));
        }
        p.setStatus(PaymentStatus.CREATED);
        return new CheckoutRes(p.getId(), p.getRazorpayOrderId(), p.getAmount().movePointRight(2).longValueExact(),
                "INR", gateway.keyId(), gateway.isMock(), p.getNote());
    }

    private void markPaid(Payment p, String gatewayPaymentId) {
        if (p.getStatus() == PaymentStatus.PAID) return;
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(Instant.now());
        p.setRazorpayPaymentId(gatewayPaymentId);
        p.setInvoiceNo(invoiceNo(p));
        WorkOrder wo = p.getWorkOrder();
        if (p.getType() == PaymentType.FAST_TRACK) {
            workOrders.grantFastTrack(wo);
        } else if (p.getType() == PaymentType.FINE) {
            events.save(new WorkOrderEvent(wo, p.getPayer().getName(), "FINE_PAID", "Penalty of Rs " + p.getAmount() + " settled", null));
            notifications.notifyAdmins("Penalty paid for " + wo.getCode(), wo.getId());
        }
    }

    private String invoiceNo(Payment p) {
        return "INV-" + Year.now().getValue() + "-" + String.format("%05d", p.getId());
    }
}
