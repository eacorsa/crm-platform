package mx.cec.crm.repository;

import mx.cec.crm.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long> {
    List<Deal> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Deal> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
