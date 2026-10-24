package com.relay.repo;

import com.relay.domain.WorkOrder;
import com.relay.domain.WorkOrderEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderEventRepository extends JpaRepository<WorkOrderEvent, Long> {
    List<WorkOrderEvent> findByWorkOrderOrderByCreatedAtAsc(WorkOrder workOrder);
}
