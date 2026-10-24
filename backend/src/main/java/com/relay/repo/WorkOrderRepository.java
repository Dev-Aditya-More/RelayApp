package com.relay.repo;

import com.relay.domain.Enums.Status;
import com.relay.domain.User;
import com.relay.domain.WorkOrder;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
    List<WorkOrder> findByCitizenOrderByCreatedAtDesc(User citizen);
    List<WorkOrder> findByContractorOrderByCreatedAtDesc(User contractor);
    List<WorkOrder> findByStatusInAndSlaBreachedFalseAndSlaDeadlineBefore(Collection<Status> statuses, Instant now);
    long countByContractorAndStatusIn(User contractor, Collection<Status> statuses);
}
