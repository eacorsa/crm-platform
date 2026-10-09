package mx.cec.crm.dto;

import jakarta.validation.constraints.NotNull;

public record TaskDoneRequest(@NotNull Boolean done) {}
