package mx.cec.crm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.config.PasswordResetSettings;
import mx.cec.crm.dto.ForgotPasswordRequest;
import mx.cec.crm.dto.ResetPasswordRequest;
import mx.cec.crm.exception.MailUnavailableException;
import mx.cec.crm.service.PasswordResetRateLimiter;
import mx.cec.crm.service.PasswordResetService;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService service;
    private final PasswordResetRateLimiter limiter;
    private final PasswordResetSettings settings;
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody ForgotPasswordRequest request,
                                                     HttpServletRequest http) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        limiter.acquireRequest(email, http.getRemoteAddr());
        if (!settings.enabled()) throw new MailUnavailableException();
        try { service.sendRecoveryLink(email); }
        catch (TaskRejectedException e) { throw new MailUnavailableException(); }
        return ResponseEntity.accepted().body(Map.of("message",
                "Si el correo corresponde a una cuenta activa, recibirás un enlace para cambiar tu contraseña."));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> reset(@Valid @RequestBody ResetPasswordRequest request,
                                                    HttpServletRequest http) {
        limiter.acquireReset(http.getRemoteAddr()); service.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada. Inicia sesión con tu nueva contraseña."));
    }
}

