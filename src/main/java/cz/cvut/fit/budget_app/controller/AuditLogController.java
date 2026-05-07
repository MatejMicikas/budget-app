package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.response.AuditLogResponse;
import cz.cvut.fit.budget_app.entity.AuditLog;
import cz.cvut.fit.budget_app.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> findAll(
            @RequestParam(required = false) AuditLog.OperationType operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (operationType != null) {
            return ResponseEntity.ok(auditLogService.findByOperationType(operationType));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(auditLogService.findByDateRange(from, to));
        }
        return ResponseEntity.ok(auditLogService.findAll());
    }
}
