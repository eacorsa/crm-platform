package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.DealRequest;
import mx.cec.crm.entity.*;
import mx.cec.crm.exception.ResourceNotFoundException;
import mx.cec.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Deal> findAll(Long userId) {
        return dealRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Deal create(DealRequest req, Long userId) {
        User user = userRepository.getReferenceById(userId);
        return dealRepository.save(Deal.builder()
                .user(user)
                .title(req.title())
                .value(req.value())
                .contactName(req.contactName())
                .stage(req.stage() != null ? req.stage() : Deal.Stage.NUEVO)
                .closeDate(req.closeDate())
                .notes(req.notes())
                .build());
    }

    @Transactional
    public Deal update(Long id, DealRequest req, Long userId) {
        Deal d = dealRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", id));
        d.setTitle(req.title());
        d.setValue(req.value());
        d.setContactName(req.contactName());
        d.setCloseDate(req.closeDate());
        if (req.stage()       != null) d.setStage(req.stage());
        d.setNotes(req.notes());
        return dealRepository.save(d);
    }

    @Transactional
    public Deal updateStage(Long id, Deal.Stage stage, Long userId) {
        Deal entity = dealRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", id));
        entity.setStage(stage);
        return dealRepository.save(entity);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (dealRepository.findByIdAndUserId(id, userId).isEmpty())
            throw new ResourceNotFoundException("Deal", id);
        dealRepository.deleteByIdAndUserId(id, userId);
    }
}
