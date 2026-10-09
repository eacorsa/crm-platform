package mx.cec.crm.service;

import mx.cec.crm.dto.CompanyRequest;
import mx.cec.crm.dto.DealRequest;
import mx.cec.crm.dto.TaskRequest;
import mx.cec.crm.entity.Company;
import mx.cec.crm.entity.Deal;
import mx.cec.crm.entity.Task;
import mx.cec.crm.exception.ResourceNotFoundException;
import mx.cec.crm.repository.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceUpdateTest {
    private final UserRepository users = mock(UserRepository.class);
    private final DealRepository deals = mock(DealRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);

    @Test
    void fullDealUpdateCanClearOptionalFields() {
        Deal existing = Deal.builder().title("Venta").value(BigDecimal.TEN)
                .closeDate(LocalDate.now()).contactName("Ana").notes("Notas").build();
        when(deals.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        new DealService(deals, users).update(1L,
                new DealRequest("Venta", null, null, Deal.Stage.NUEVO, null, null), 7L);
        assertNull(existing.getValue());
        assertNull(existing.getCloseDate());
        assertNull(existing.getContactName());
        assertNull(existing.getNotes());
        verify(deals).save(existing);
    }

    @Test
    void fullTaskUpdateCanClearDueDate() {
        Task existing = Task.builder().title("Llamar").dueDate(LocalDate.now()).build();
        when(tasks.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        new TaskService(tasks, users).update(1L,
                new TaskRequest("Llamar", Task.Type.LLAMADA, Task.Priority.NORMAL, null, null, null, false), 7L);
        assertNull(existing.getDueDate());
        verify(tasks).save(existing);
    }

    @Test
    void fullCompanyUpdateCanClearWebsite() {
        Company existing = Company.builder().name("Empresa").website("https://example.com").build();
        when(companies.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        new CompanyService(companies, users).update(1L, new CompanyRequest("Empresa", null, null, null, null), 7L);
        assertNull(existing.getWebsite());
        verify(companies).save(existing);
    }

    @Test
    void stagePatchPreservesOtherFields() {
        Deal existing = Deal.builder().title("Venta").value(BigDecimal.TEN).closeDate(LocalDate.now()).build();
        when(deals.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        new DealService(deals, users).updateStage(1L, Deal.Stage.CERRADO, 7L);
        assertEquals("Venta", existing.getTitle());
        assertEquals(BigDecimal.TEN, existing.getValue());
        assertNotNull(existing.getCloseDate());
        assertEquals(Deal.Stage.CERRADO, existing.getStage());
    }

    @Test
    void completionPatchPreservesOtherFields() {
        Task existing = Task.builder().title("Llamar").dueDate(LocalDate.now()).build();
        when(tasks.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));
        new TaskService(tasks, users).updateDone(1L, true, 7L);
        assertEquals("Llamar", existing.getTitle());
        assertNotNull(existing.getDueDate());
        assertTrue(existing.isDone());
    }

    @Test
    void patchesCannotModifyAnotherOwnersResource() {
        when(deals.findByIdAndUserId(1L, 8L)).thenReturn(Optional.empty());
        when(tasks.findByIdAndUserId(1L, 8L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> new DealService(deals, users).updateStage(1L, Deal.Stage.CERRADO, 8L));
        assertThrows(ResourceNotFoundException.class,
                () -> new TaskService(tasks, users).updateDone(1L, true, 8L));
        verify(deals, never()).save(any());
        verify(tasks, never()).save(any());
    }
}
