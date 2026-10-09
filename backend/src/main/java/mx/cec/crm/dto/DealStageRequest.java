package mx.cec.crm.dto;

import jakarta.validation.constraints.NotNull;
import mx.cec.crm.entity.Deal;

public record DealStageRequest(@NotNull Deal.Stage stage) {}
