package com.relay.domain;

import com.relay.domain.Enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) private PaymentType type;
    @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PENDING;

    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY) private User payer;
    @ManyToOne(fetch = FetchType.LAZY) private User payee;
    @ManyToOne(fetch = FetchType.LAZY) private WorkOrder workOrder;

    @Column(unique = true) private String razorpayOrderId;
    private String razorpayPaymentId;
    private String invoiceNo;
    private String note;

    private Instant createdAt = Instant.now();
    private Instant paidAt;
}
