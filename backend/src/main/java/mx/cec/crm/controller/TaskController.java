package mx.cec.crm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.TaskRequest;
import mx.cec.crm.dto.TaskDoneRequest;
import mx.cec.crm.entity.Task;
import mx.cec.crm.service.TaskService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<Task> list(@AuthenticationPrincipal UserDetails principal) {
        return taskService.findAll(userId(principal));
    }

    @PostMapping
    public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest req,
                                       @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(req, userId(principal)));
    }

    @PutMapping("/{id}")
    public Task update(@PathVariable Long id,
                       @Valid @RequestBody TaskRequest req,
                       @AuthenticationPrincipal UserDetails principal) {
        return taskService.update(id, req, userId(principal));
    }

    @PatchMapping("/{id}/done")
    public Task updateDone(@PathVariable Long id,
                         @Valid @RequestBody TaskDoneRequest req,
                         @AuthenticationPrincipal UserDetails principal) {
        return taskService.updateDone(id, req.done(), userId(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails principal) {
        taskService.delete(id, userId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long userId(UserDetails p) { return Long.parseLong(p.getUsername()); }
}
