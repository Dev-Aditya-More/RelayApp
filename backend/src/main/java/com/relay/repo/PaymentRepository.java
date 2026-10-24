package com.relay.repo;

import com.relay.domain.Enums.PaymentType;
import com.relay.domain.Payment;
import com.relay.domain.User;
import com.relay.domain.WorkOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String orderId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByPayerOrPayeeOrderByCreatedAtDesc(User payer, User payee);
    List<Payment> findByWorkOrder(WorkOrder workOrder);
    boolean existsByWorkOrderAndType(WorkOrder workOrder, PaymentType type);
}
