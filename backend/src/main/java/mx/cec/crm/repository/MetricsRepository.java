package mx.cec.crm.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.MetricsResponse.DealStageMetric;
import mx.cec.crm.dto.MetricsResponse.TaskStatusMetric;
import mx.cec.crm.entity.Deal;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Aggregate inside the database; never load full CRM records for a chart. */
@Repository
@RequiredArgsConstructor
public class MetricsRepository {
    private final EntityManager entityManager;

    public List<DealStageMetric> dealStages(Long userId) {
        return entityManager.createQuery("""
                select d.stage, count(d), sum(d.value) from Deal d
                where d.user.id = :userId group by d.stage
                """, Object[].class).setParameter("userId", userId).getResultList().stream()
                .map(row -> new DealStageMetric((Deal.Stage) row[0], ((Number) row[1]).longValue(),
                        row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2])).toList();
    }

    public List<TaskStatusMetric> taskStatuses(Long userId) {
        return entityManager.createQuery("""
                select t.done, count(t) from Task t where t.user.id = :userId group by t.done
                """, Object[].class).setParameter("userId", userId).getResultList().stream()
                .map(row -> new TaskStatusMetric((Boolean) row[0], ((Number) row[1]).longValue())).toList();
    }

    public long countContacts(Long userId) {
        return entityManager.createQuery("select count(c) from Contact c where c.user.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult();
    }

    public long countCompanies(Long userId) {
        return entityManager.createQuery("select count(c) from Company c where c.user.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult();
    }

    public Map<YearMonth, Long> contactGrowth(Long userId, LocalDateTime start, LocalDateTime end) {
        return growth("Contact", userId, start, end);
    }

    public Map<YearMonth, Long> companyGrowth(Long userId, LocalDateTime start, LocalDateTime end) {
        return growth("Company", userId, start, end);
    }

    private Map<YearMonth, Long> growth(String entity, Long userId, LocalDateTime start, LocalDateTime end) {
        // Entity name is an internal constant; every external value is a bound parameter.
        return entityManager.createQuery("select year(c.createdAt), month(c.createdAt), count(c) from " + entity + " c " +
                "where c.user.id = :userId and c.createdAt >= :start and c.createdAt < :end " +
                "group by year(c.createdAt), month(c.createdAt)", Object[].class)
                .setParameter("userId", userId).setParameter("start", start).setParameter("end", end)
                .getResultList().stream().collect(Collectors.toMap(
                        row -> YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        row -> ((Number) row[2]).longValue()));
    }
}
