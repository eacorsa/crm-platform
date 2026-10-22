package mx.cec.crm.controller;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.MetricsResponse;
import mx.cec.crm.service.MetricsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsService service;

    @GetMapping
    public MetricsResponse overview(@AuthenticationPrincipal UserDetails principal,
                                    @RequestParam(defaultValue = "6") int months) {
        return service.overview(Long.parseLong(principal.getUsername()), months);
    }
}
