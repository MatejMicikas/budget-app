package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.response.AuditLogResponse;
import cz.cvut.fit.budget_app.entity.AuditLog;
import cz.cvut.fit.budget_app.service.AuditLogService;
import cz.cvut.fit.budget_app.util.AuditDateTimeParam;
import lombok.RequiredArgsConstructor;
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
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime fromDt = safeParseBound(from);
        LocalDateTime toDt = safeParseBound(to);

        boolean hasOp = operationType != null && !operationType.isBlank();
        boolean hasRange = fromDt != null && toDt != null;

        if (hasRange && fromDt.isAfter(toDt)) {
            LocalDateTime tmp = fromDt;
            fromDt = toDt;
            toDt = tmp;
        }

        if (hasOp && hasRange) {
            try {
                AuditLog.OperationType op = AuditLog.OperationType.valueOf(operationType.trim());
                return ResponseEntity.ok(auditLogService.findByOperationTypeAndDateRange(op, fromDt, toDt));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid operationType: use a full enum constant name (e.g. USER_ROLE_CHANGED), not a substring.");
            }
        }
        if (hasOp) {
            try {
                AuditLog.OperationType op = AuditLog.OperationType.valueOf(operationType.trim());
                return ResponseEntity.ok(auditLogService.findByOperationType(op));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid operationType: use a full enum constant name (e.g. USER_ROLE_CHANGED), not a substring.");
            }
        }
        if (hasRange) {
            return ResponseEntity.ok(auditLogService.findByDateRange(fromDt, toDt));
        }
        return ResponseEntity.ok(auditLogService.findAll());
    }

    private static LocalDateTime safeParseBound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return AuditDateTimeParam.parseOptional(raw);
    }
}
