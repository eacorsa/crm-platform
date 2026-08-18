package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.CompanyRequest;
import mx.cec.crm.entity.*;
import mx.cec.crm.exception.ResourceNotFoundException;
import mx.cec.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository    userRepository;

    @Transactional(readOnly = true)
    public List<Company> findAll(Long userId) {
        return companyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Company create(CompanyRequest req, Long userId) {
        User user = userRepository.getReferenceById(userId);
        return companyRepository.save(Company.builder()
                .user(user).name(req.name()).industry(req.industry())
                .website(req.website()).phone(req.phone()).notes(req.notes())
                .build());
    }

    @Transactional
    public Company update(Long id, CompanyRequest req, Long userId) {
        Company c = companyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
        c.setName(req.name());
        if (req.industry() != null) c.setIndustry(req.industry());
        if (req.website()  != null) c.setWebsite(req.website());
        if (req.phone()    != null) c.setPhone(req.phone());
        if (req.notes()    != null) c.setNotes(req.notes());
        return companyRepository.save(c);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (companyRepository.findByIdAndUserId(id, userId).isEmpty())
            throw new ResourceNotFoundException("Empresa", id);
        companyRepository.deleteByIdAndUserId(id, userId);
    }
}
