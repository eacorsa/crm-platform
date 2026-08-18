package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.ContactRequest;
import mx.cec.crm.entity.*;
import mx.cec.crm.exception.ResourceNotFoundException;
import mx.cec.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository    userRepository;

    @Transactional(readOnly = true)
    public List<Contact> findAll(Long userId) {
        return contactRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Contact create(ContactRequest req, Long userId) {
        User user = userRepository.getReferenceById(userId);
        Contact contact = Contact.builder()
                .user(user)
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .company(req.company())
                .stage(req.stage() != null ? req.stage() : Contact.Stage.LEAD)
                .source(req.source())
                .notes(req.notes())
                .build();
        return contactRepository.save(contact);
    }

    @Transactional
    public Contact update(Long id, ContactRequest req, Long userId) {
        Contact contact = contactRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto", id));
        contact.setName(req.name());
        contact.setEmail(req.email());
        contact.setPhone(req.phone());
        contact.setCompany(req.company());
        if (req.stage() != null) contact.setStage(req.stage());
        contact.setSource(req.source());
        contact.setNotes(req.notes());
        return contactRepository.save(contact);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (contactRepository.findByIdAndUserId(id, userId).isEmpty())
            throw new ResourceNotFoundException("Contacto", id);
        contactRepository.deleteByIdAndUserId(id, userId);
    }
}
