package mx.cec.crm.controller;

import mx.cec.crm.config.PasswordResetSettings;
import mx.cec.crm.config.SecurityConfig;
import mx.cec.crm.security.JwtAuthFilter;
import mx.cec.crm.security.JwtTokenProvider;
import mx.cec.crm.service.PasswordResetRateLimiter;
import mx.cec.crm.service.PasswordResetService;
import mx.cec.crm.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PasswordResetController.class,
        properties = "app.cors.allowed-origins=http://127.0.0.1:5173")
@Import({SecurityConfig.class, JwtAuthFilter.class})
class PasswordResetContractTest {
    @Autowired MockMvc mvc;
    @MockBean PasswordResetService service;
    @MockBean PasswordResetRateLimiter limiter;
    @MockBean PasswordResetSettings settings;
    @MockBean JwtTokenProvider jwt;
    @MockBean UserDetailsService users;
    private static final String TOKEN = "a".repeat(43);

    @Test
    void publicRequestNormalizesEmailAndDoesNotExposeAccount() throws Exception {
        when(settings.enabled()).thenReturn(true);
        mvc.perform(post("/auth/forgot-password").servletPath("/auth/forgot-password")
                .header("Authorization", "Bearer stale").contentType("application/json")
                .content("{\"email\":\"Person@Example.com\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.token").doesNotExist());
        verify(service).sendRecoveryLink("person@example.com");
        verifyNoInteractions(jwt, users);
    }
    @Test
    void resetIsPublicAndIgnoresOldBearer() throws Exception {
        mvc.perform(post("/auth/reset-password").servletPath("/auth/reset-password")
                .header("Authorization", "Bearer stale").contentType("application/json")
                .content("{\"token\":\"" + TOKEN + "\",\"password\":\"new-password\"}"))
                .andExpect(status().isOk());
        verify(service).resetPassword(any()); verifyNoInteractions(jwt, users);
    }
    @Test
    void invalidInputsNeverReachService() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType("application/json").content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/reset-password").contentType("application/json").content("{\"token\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.password").exists());
        verifyNoInteractions(service, limiter);
    }
    @Test
    void disabledMailIsAnExplicit503() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType("application/json").content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isServiceUnavailable());
        verifyNoInteractions(service);
    }
    @Test
    void fullMailQueueReturns503WithoutLookingUpAccount() throws Exception {
        when(settings.enabled()).thenReturn(true);
        doThrow(new TaskRejectedException("full")).when(service).sendRecoveryLink(anyString());
        mvc.perform(post("/auth/forgot-password").contentType("application/json").content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isServiceUnavailable());
    }
    @Test
    void expiredLinkReturnsReadable400() throws Exception {
        doThrow(new IllegalArgumentException("El enlace es inválido o ha vencido.")).when(service).resetPassword(any());
        mvc.perform(post("/auth/reset-password").contentType("application/json")
                .content("{\"token\":\"" + TOKEN + "\",\"password\":\"new-password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }
    @Test
    void requestLimitReturnsRetryAfter() throws Exception {
        doThrow(new RateLimitExceededException(30)).when(limiter).acquireRequest(anyString(), anyString());
        mvc.perform(post("/auth/forgot-password").contentType("application/json").content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));
        verifyNoInteractions(service);
    }
    @ParameterizedTest
    @ValueSource(strings = {"/auth/forgot-password", "/auth/reset-password"})
    void onlyPostIsPublicAndCorsAllowsIt(String endpoint) throws Exception {
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(options(endpoint).header("Origin", "http://127.0.0.1:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }
}

