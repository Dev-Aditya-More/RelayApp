package com.relay.repo;

import com.relay.domain.PasswordResetToken;
import com.relay.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    List<PasswordResetToken> findByUserAndUsedAtIsNull(User user);
    Optional<PasswordResetToken> findFirstByUserOrderByCreatedAtDesc(User user);
}
