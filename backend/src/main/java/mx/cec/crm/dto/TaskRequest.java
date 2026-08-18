package mx.cec.crm.dto;

import jakarta.validation.constraints.*;
import mx.cec.crm.entity.Task;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
                                   Task.Type type,
                                   Task.Priority priority,
        @Size(max = 100)           String contactName,
                                   LocalDate dueDate,
        @Size(max = 2000)          String notes,
                                   Boolean done
) {}
