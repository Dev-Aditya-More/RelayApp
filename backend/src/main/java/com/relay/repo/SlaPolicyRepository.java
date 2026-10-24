package com.relay.repo;

import com.relay.domain.Enums.Category;
import com.relay.domain.SlaPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {
    Optional<SlaPolicy> findByCategory(Category category);
}
