package mx.cec.crm.controller;

import mx.cec.crm.config.SecurityConfig;
import mx.cec.crm.entity.Deal;
import mx.cec.crm.entity.Task;
import mx.cec.crm.exception.RateLimitExceededException;
import mx.cec.crm.security.JwtAuthFilter;
import mx.cec.crm.security.JwtTokenProvider;
import mx.cec.crm.service.AuthService;
import mx.cec.crm.service.DealService;
import mx.cec.crm.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import mx.cec.crm.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {TaskController.class, DealController.class, AuthController.class},
        properties = "app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5173")
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ApiContractTest {
    @Autowired MockMvc mvc;
    @MockBean TaskService tasks;
    @MockBean DealService deals;
    @MockBean AuthService auth;
    @MockBean JwtTokenProvider tokens;
    @MockBean UserDetailsService users;

    @Test
    @WithMockUser(username = "7")
    void partialTaskCompletionDoesNotRequireTitle() throws Exception {
        when(tasks.updateDone(3L, true, 7L)).thenReturn(Task.builder().id(3L).title("Llamar").done(true).build());
        mvc.perform(patch("/tasks/3/done").contentType("application/json").content("{\"done\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.done").value(true));
        verify(tasks).updateDone(3L, true, 7L);
    }

    @Test
    @WithMockUser(username = "7")
    void partialStageChangeDoesNotRequireTitle() throws Exception {
        when(deals.updateStage(4L, Deal.Stage.CERRADO, 7L))
                .thenReturn(Deal.builder().id(4L).title("Venta").stage(Deal.Stage.CERRADO).build());
        mvc.perform(patch("/deals/4/stage").contentType("application/json").content("{\"stage\":\"CERRADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("CERRADO"));
        verify(deals).updateStage(4L, Deal.Stage.CERRADO, 7L);
    }

    @Test
    @WithMockUser(username = "7")
    void emptyPatchIsRejected() throws Exception {
        mvc.perform(patch("/tasks/3/done").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.done").exists());
        mvc.perform(patch("/deals/4/stage").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.stage").exists());
        verifyNoInteractions(tasks, deals);
    }

    @Test
    @WithMockUser(username = "7")
    void creationAndFullUpdateStillRequireTitle() throws Exception {
        mvc.perform(post("/tasks").contentType("application/json").content("{\"done\":true}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        mvc.perform(put("/deals/4").contentType("application/json").content("{\"stage\":\"NUEVO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        verifyNoInteractions(tasks, deals);
    }

    @Test
    @WithMockUser(username = "7")
    void malformedEnumDateAndJsonReturn400() throws Exception {
        mvc.perform(patch("/deals/4/stage").contentType("application/json").content("{\"stage\":\"INVALID\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        mvc.perform(put("/tasks/3").contentType("application/json").content("{\"title\":\"Tarea\",\"dueDate\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/tasks").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(tasks, deals);
    }

    @Test
    void protectedRouteWithoutTokenReturnsJson401() throws Exception {
        mvc.perform(get("/tasks")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(tasks);
    }

    @Test
    void invalidOrExpiredTokenReturns401() throws Exception {
        when(tokens.validateToken("expired")).thenReturn(false);
        mvc.perform(get("/tasks").header("Authorization", "Bearer expired"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(tasks, users);
    }

    @Test
    void deletedOrInactiveAccountReturns401() throws Exception {
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.getUserIdFromToken("valid")).thenReturn(7L);
        when(users.loadUserByUsername("7")).thenThrow(new UsernameNotFoundException("inactive"));
        mvc.perform(get("/tasks").header("Authorization", "Bearer valid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(tasks);
    }

    @Test
    void validTokenUsesOwnerFromPrincipal() throws Exception {
        when(tokens.validateToken("valid")).thenReturn(true);
        when(tokens.getUserIdFromToken("valid")).thenReturn(7L);
        when(users.loadUserByUsername("7")).thenReturn(User.withUsername("7").password("unused").roles("AGENT").build());
        when(tokens.matchesCredentials("valid", "unused")).thenReturn(true);
        when(tasks.findAll(7L)).thenReturn(List.of());
        mvc.perform(get("/tasks").header("Authorization", "Bearer valid"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(tasks).findAll(7L);
    }

    @Test
    void loginIgnoresStaleBearerAndReturnsCredentialsError() throws Exception {
        when(auth.login(any())).thenThrow(new BadCredentialsException("bad"));
        mvc.perform(post("/auth/login").servletPath("/auth/login").header("Authorization", "Bearer stale")
                        .contentType("application/json").content("{\"email\":\"a@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Credenciales incorrectas."));
        verifyNoInteractions(tokens, users);
    }

    @Test
    void rateLimitReturns429AndRetryAfter() throws Exception {
        when(auth.login(any())).thenThrow(new RateLimitExceededException(30));
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"a@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "30"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173"})
    void corsAllowsPatchWithoutAuthentication(String origin) throws Exception {
        mvc.perform(options("/tasks/3/done").header("Origin", origin)
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173"})
    void loginFromAllowedBrowserOriginsReturnsToken(String origin) throws Exception {
        when(auth.login(any())).thenReturn(new AuthResponse("test-token", "Admin", "a@example.com", "ADMIN"));
        String payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                java.util.Map.of("email", "a@example.com", "password", "test-password"));
        mvc.perform(post("/auth/login").header("Origin", origin).contentType("application/json").content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    void loginFromUntrustedOriginIsRejectedBeforeAuthentication() throws Exception {
        mvc.perform(post("/auth/login").header("Origin", "https://untrusted.example")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }
}
