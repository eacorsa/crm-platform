package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.TaskRequest;
import mx.cec.crm.entity.*;
import mx.cec.crm.exception.ResourceNotFoundException;
import mx.cec.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Task> findAll(Long userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Task create(TaskRequest req, Long userId) {
        User user = userRepository.getReferenceById(userId);
        return taskRepository.save(Task.builder()
                .user(user)
                .title(req.title())
                .type(req.type() != null ? req.type() : Task.Type.LLAMADA)
                .priority(req.priority() != null ? req.priority() : Task.Priority.NORMAL)
                .contactName(req.contactName())
                .dueDate(req.dueDate())
                .notes(req.notes())
                .done(false)
                .build());
    }

    @Transactional
    public Task update(Long id, TaskRequest req, Long userId) {
        Task t = taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea", id));
        if (req.title()       != null) t.setTitle(req.title());
        if (req.type()        != null) t.setType(req.type());
        if (req.priority()    != null) t.setPriority(req.priority());
        if (req.contactName() != null) t.setContactName(req.contactName());
        if (req.dueDate()     != null) t.setDueDate(req.dueDate());
        if (req.notes()       != null) t.setNotes(req.notes());
        if (req.done()        != null) t.setDone(req.done());
        return taskRepository.save(t);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (taskRepository.findByIdAndUserId(id, userId).isEmpty())
            throw new ResourceNotFoundException("Tarea", id);
        taskRepository.deleteByIdAndUserId(id, userId);
    }
}
