package mx.cec.crm.controller;

import mx.cec.crm.config.SecurityConfig;
import mx.cec.crm.dto.MetricsResponse;
import mx.cec.crm.security.JwtAuthFilter;
import mx.cec.crm.security.JwtTokenProvider;
import mx.cec.crm.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MetricsController.class, properties = "app.cors.allowed-origins=http://127.0.0.1:5173")
@Import({SecurityConfig.class, JwtAuthFilter.class})
class MetricsContractTest {
    @Autowired MockMvc mvc;
    @MockBean MetricsService service;
    @MockBean JwtTokenProvider jwt;
    @MockBean UserDetailsService users;

    private MetricsResponse empty() {
        return new MetricsResponse(LocalDate.of(2026, 1, 15),
                new MetricsResponse.Summary(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0), List.of(), List.of(), List.of());
    }
    @Test
    void anonymousCannotReadMetrics() throws Exception {
        mvc.perform(get("/metrics")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(service);
    }
    @Test
    @WithMockUser(username = "7")
    void defaultPeriodUsesAuthenticatedOwnerEvenIfClientSuppliesAnotherId() throws Exception {
        when(service.overview(7L, 6)).thenReturn(empty());
        mvc.perform(get("/metrics").param("userId", "99"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.contacts").value(0));
        verify(service).overview(7L, 6);
    }
    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminStillUsesOwnRecordsAndCanRequestTwelveMonths() throws Exception {
        when(service.overview(7L, 12)).thenReturn(empty());
        mvc.perform(get("/metrics").param("months", "12")).andExpect(status().isOk());
        verify(service).overview(7L, 12);
    }
    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "99999999999999999"})
    @WithMockUser(username = "7")
    void nonIntegerPeriodReturns400(String value) throws Exception {
        mvc.perform(get("/metrics").param("months", value)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test
    @WithMockUser(username = "7")
    void unsupportedPeriodReturns400() throws Exception {
        when(service.overview(7L, 13)).thenThrow(new IllegalArgumentException("El periodo debe ser de 6 o 12 meses."));
        mvc.perform(get("/metrics").param("months", "13")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
    }
}
