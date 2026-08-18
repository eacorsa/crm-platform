package mx.cec.crm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.CompanyRequest;
import mx.cec.crm.entity.Company;
import mx.cec.crm.service.CompanyService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public List<Company> list(@AuthenticationPrincipal UserDetails principal) {
        return companyService.findAll(userId(principal));
    }

    @PostMapping
    public ResponseEntity<Company> create(@Valid @RequestBody CompanyRequest req,
                                          @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(req, userId(principal)));
    }

    @PutMapping("/{id}")
    public Company update(@PathVariable Long id,
                          @Valid @RequestBody CompanyRequest req,
                          @AuthenticationPrincipal UserDetails principal) {
        return companyService.update(id, req, userId(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        companyService.delete(id, userId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long userId(UserDetails p) { return Long.parseLong(p.getUsername()); }
}
