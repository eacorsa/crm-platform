package mx.cec.crm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.DealRequest;
import mx.cec.crm.entity.Deal;
import mx.cec.crm.service.DealService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public List<Deal> list(@AuthenticationPrincipal UserDetails principal) {
        return dealService.findAll(userId(principal));
    }

    @PostMapping
    public ResponseEntity<Deal> create(@Valid @RequestBody DealRequest req,
                                       @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dealService.create(req, userId(principal)));
    }

    @PutMapping("/{id}")
    public Deal update(@PathVariable Long id,
                       @Valid @RequestBody DealRequest req,
                       @AuthenticationPrincipal UserDetails principal) {
        return dealService.update(id, req, userId(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        dealService.delete(id, userId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long userId(UserDetails p) { return Long.parseLong(p.getUsername()); }
}
