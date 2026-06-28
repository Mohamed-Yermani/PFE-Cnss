package com.projet.cnss.services;

import com.projet.cnss.entity.AuditLog;
import com.projet.cnss.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // ==========================================================
    // log
    // ==========================================================

    @Test
    void log_success_savesAuditLogWithActionDetailsAndTimestamp() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        auditLogService.log("CREATE_USER", "Admin a créé l'utilisateur jean@test.com");
        LocalDateTime after = LocalDateTime.now();

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals("CREATE_USER", saved.getAction());
        assertEquals("Admin a créé l'utilisateur jean@test.com", saved.getDetails());
        assertNotNull(saved.getTimestamp());
        assertFalse(saved.getTimestamp().isBefore(before));
        assertFalse(saved.getTimestamp().isAfter(after));
    }

    @Test
    void log_withNullDetails_savesAuditLogWithNullDetails() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditLogService.log("DELETE_USER", null);

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals("DELETE_USER", saved.getAction());
        assertNull(saved.getDetails());
        assertNotNull(saved.getTimestamp());
    }

    // ==========================================================
    // getAllLogs
    // ==========================================================

    @Test
    void getAllLogs_returnsAllLogs() {
        AuditLog log1 = new AuditLog(1L, "CREATE_USER", "détail 1", LocalDateTime.now());
        AuditLog log2 = new AuditLog(2L, "DEACTIVATE_AGENT", "détail 2", LocalDateTime.now());

        when(auditLogRepository.findAll()).thenReturn(List.of(log1, log2));

        List<AuditLog> result = auditLogService.getAllLogs();

        assertEquals(2, result.size());
        assertEquals("CREATE_USER", result.get(0).getAction());
        assertEquals("DEACTIVATE_AGENT", result.get(1).getAction());
    }

    @Test
    void getAllLogs_emptyList_returnsEmptyList() {
        when(auditLogRepository.findAll()).thenReturn(List.of());

        List<AuditLog> result = auditLogService.getAllLogs();

        assertTrue(result.isEmpty());
    }
}