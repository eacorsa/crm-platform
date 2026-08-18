package mx.cec.crm.dto;

import jakarta.validation.constraints.*;
import mx.cec.crm.entity.Contact;

public record ContactRequest(
        @NotBlank @Size(max = 150) String name,
        @Email @Size(max = 180)    String email,
        @Size(max = 30)            String phone,
        @Size(max = 150)           String company,
                                   Contact.Stage stage,
        @Size(max = 60)            String source,
        @Size(max = 2000)          String notes
) {}
