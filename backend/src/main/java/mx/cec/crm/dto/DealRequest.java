package mx.cec.crm.dto;

import jakarta.validation.constraints.*;
import mx.cec.crm.entity.Deal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DealRequest(
        @NotBlank @Size(max = 200)                          String title,
        @DecimalMin("0") @Digits(integer=12, fraction=2)    BigDecimal value,
        @Size(max = 100)                                    String contactName,
                                                            Deal.Stage stage,
                                                            LocalDate closeDate,
        @Size(max = 2000)                                   String notes
) {}
