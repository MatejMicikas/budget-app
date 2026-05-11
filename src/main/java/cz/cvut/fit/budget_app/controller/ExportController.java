package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/transactions/{seasonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<byte[]> exportTransactions(@PathVariable Long seasonId,
                                                     @RequestParam(required = false) Transaction.ApprovalStatus status,
                                                     @RequestParam(defaultValue = "false") boolean includeProposed) {
        String csv = exportService.exportTransactionsCsv(seasonId, status, includeProposed);
        return csvResponse(csv, "transactions-season-" + seasonId + ".csv");
    }

    @GetMapping("/budget-items/{seasonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<byte[]> exportBudgetItems(@PathVariable Long seasonId) {
        String csv = exportService.exportBudgetItemsCsv(seasonId);
        return csvResponse(csv, "budget-items-season-" + seasonId + ".csv");
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
