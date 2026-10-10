package mx.cec.crm.repository;

import mx.cec.crm.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.userId = :id")
    Optional<PasswordResetToken> findByUserIdForUpdate(@Param("id") Long id);

    // Avoid caching a stale entity before acquiring the user lock.
    @Query("select t.userId from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByTokenHash(@Param("hash") String hash);
}

