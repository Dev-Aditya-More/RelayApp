package com.relay.repo;

import com.relay.domain.EmailVerificationToken;
import com.relay.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    List<EmailVerificationToken> findByUserAndUsedAtIsNull(User user);
    Optional<EmailVerificationToken> findFirstByUserOrderByCreatedAtDesc(User user);
}
