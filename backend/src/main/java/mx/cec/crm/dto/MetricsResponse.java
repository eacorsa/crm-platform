package mx.cec.crm.dto;

import mx.cec.crm.entity.Deal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MetricsResponse(LocalDate asOf, Summary summary, List<DealStageMetric> dealStages,
                              List<TaskStatusMetric> taskStatuses, List<MonthlyGrowth> growth) {
    public record Summary(long contacts, long companies, long deals, BigDecimal openValue,
                          BigDecimal closedValue, long pendingTasks, long completedTasks) {}
    public record DealStageMetric(Deal.Stage stage, long count, BigDecimal value) {}
    public record TaskStatusMetric(boolean done, long count) {}
    public record MonthlyGrowth(String month, long contacts, long companies) {}
}
