package mx.cec.crm.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.password-reset")
public record PasswordResetSettings(boolean enabled, @NotBlank @Email String from,
                                    @NotBlank String resetUrl, @Min(1) @Max(60) int ttlMinutes) {}

