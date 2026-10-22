package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.MetricsResponse;
import mx.cec.crm.dto.MetricsResponse.*;
import mx.cec.crm.entity.Deal;
import mx.cec.crm.repository.MetricsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private static final ZoneId REPORT_ZONE = ZoneId.of("America/Mexico_City");
    private final MetricsRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MetricsResponse overview(Long userId, int months) {
        if (months != 6 && months != 12)
            throw new IllegalArgumentException("El periodo debe ser de 6 o 12 meses.");
        LocalDate today = LocalDate.now(clock.withZone(REPORT_ZONE));
        YearMonth current = YearMonth.from(today);
        YearMonth first = current.minusMonths(months - 1L);
        LocalDateTime start = first.atDay(1).atStartOfDay();
        LocalDateTime end = current.plusMonths(1).atDay(1).atStartOfDay();
        Map<Deal.Stage, DealStageMetric> grouped = new EnumMap<>(Deal.Stage.class);
        repository.dealStages(userId).forEach(row -> grouped.put(row.stage(), row));
        List<DealStageMetric> stages = Arrays.stream(Deal.Stage.values())
                .map(stage -> grouped.getOrDefault(stage, new DealStageMetric(stage, 0, BigDecimal.ZERO))).toList();
        Map<Boolean, Long> taskCounts = new HashMap<>();
        repository.taskStatuses(userId).forEach(row -> taskCounts.put(row.done(), row.count()));
        long pending = taskCounts.getOrDefault(false, 0L);
        long completed = taskCounts.getOrDefault(true, 0L);
        BigDecimal openValue = stages.stream()
                .filter(row -> row.stage() != Deal.Stage.CERRADO && row.stage() != Deal.Stage.PERDIDO)
                .map(DealStageMetric::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closedValue = grouped.getOrDefault(Deal.Stage.CERRADO,
                new DealStageMetric(Deal.Stage.CERRADO, 0, BigDecimal.ZERO)).value();
        var contacts = repository.contactGrowth(userId, start, end);
        var companies = repository.companyGrowth(userId, start, end);
        List<MonthlyGrowth> growth = new ArrayList<>();
        for (int index = 0; index < months; index++) {
            YearMonth month = first.plusMonths(index);
            growth.add(new MonthlyGrowth(month.toString(), contacts.getOrDefault(month, 0L), companies.getOrDefault(month, 0L)));
        }
        Summary summary = new Summary(repository.countContacts(userId), repository.countCompanies(userId),
                stages.stream().mapToLong(DealStageMetric::count).sum(), openValue, closedValue, pending, completed);
        return new MetricsResponse(today, summary, stages,
                List.of(new TaskStatusMetric(false, pending), new TaskStatusMetric(true, completed)), List.copyOf(growth));
    }
}
