package mx.cec.crm.dto;

import jakarta.validation.constraints.*;

public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 180) String email) {}

