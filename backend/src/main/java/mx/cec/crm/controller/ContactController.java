package mx.cec.crm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.ContactRequest;
import mx.cec.crm.entity.Contact;
import mx.cec.crm.service.ContactService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public List<Contact> list(@AuthenticationPrincipal UserDetails principal) {
        return contactService.findAll(userId(principal));
    }

    @PostMapping
    public ResponseEntity<Contact> create(@Valid @RequestBody ContactRequest req,
                                          @AuthenticationPrincipal UserDetails principal) {
        Contact created = contactService.create(req, userId(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Contact update(@PathVariable Long id,
                          @Valid @RequestBody ContactRequest req,
                          @AuthenticationPrincipal UserDetails principal) {
        return contactService.update(id, req, userId(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        contactService.delete(id, userId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long userId(UserDetails p) { return Long.parseLong(p.getUsername()); }
}
