package com.relay.web;

import com.relay.domain.Enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterReq(@NotBlank String name, @Email @NotBlank String email,
                              @Size(min = 6) String password, String phone, Role role) {}

    public record LoginReq(@NotBlank String email, @NotBlank String password) {}

    public record UserView(Long id, String name, String email, Role role, String phone) {}

    public record AuthRes(String token, UserView user) {}

    public record AssignReq(@NotNull Long contractorId) {}

    public record RateReq(@Min(1) @Max(5) int rating, String feedback) {}

    public record ClassifyReq(@NotBlank String text) {}

    public record Suggestion(Category category, Severity severity, int confidence, List<String> matched) {}

    public record WorkOrderView(
            Long id, String code, String title, String description,
            Category category, Severity severity, Status status,
            String area, String address, Double latitude, Double longitude, String photoUrl,
            String citizenName, UserView contractor, String department,
            boolean fastTrack, boolean slaBreached, boolean escalated, boolean contractorPaid, boolean fineIssued,
            Instant createdAt, Instant assignedAt, Instant startedAt, Instant completedAt, Instant slaDeadline,
            int slaHours, BigDecimal fineAmount, BigDecimal payoutAmount, BigDecimal fastTrackFee,
            Integer delayRisk, Integer rating, String feedback, String sentiment) {}

    public record EventView(String actor, String kind, String message, String photoUrl, Instant createdAt) {}

    public record WorkOrderDetail(WorkOrderView order, List<EventView> events, List<PaymentView> payments) {}

    public record PaymentView(Long id, PaymentType type, PaymentStatus status, BigDecimal amount,
                              String payerName, String payeeName, Long workOrderId, String workOrderCode,
                              String workOrderTitle, String razorpayOrderId, String razorpayPaymentId,
                              String invoiceNo, String note, Instant createdAt, Instant paidAt) {}

    public record CheckoutRes(Long paymentId, String orderId, long amountPaise, String currency,
                              String keyId, boolean mock, String description) {}

    public record VerifyReq(@NotBlank String razorpayOrderId, @NotBlank String razorpayPaymentId,
                            @NotBlank String razorpaySignature) {}

    public record PolicyReq(@Min(1) int hours, @NotNull BigDecimal fineAmount,
                            @NotNull BigDecimal contractorRate, @NotNull BigDecimal fastTrackFee) {}

    public record PolicyView(Long id, Category category, String department, int hours,
                             BigDecimal fineAmount, BigDecimal contractorRate, BigDecimal fastTrackFee) {}

    public record NotificationView(Long id, String message, Long workOrderId, boolean seen, Instant createdAt) {}
}
