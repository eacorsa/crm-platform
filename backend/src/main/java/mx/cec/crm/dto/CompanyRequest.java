package mx.cec.crm.dto;

import jakarta.validation.constraints.*;

public record CompanyRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 80)            String industry,
        @Size(max = 255)           String website,
        @Size(max = 30)            String phone,
        @Size(max = 2000)          String notes
) {}
