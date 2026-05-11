package cz.cvut.fit.budget_app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update of editable fields. Workflow-changing operations are forbidden here:
 * use {@code POST .../approve}, {@code .../reject}, {@code .../realize} instead.
 * <p>
 * Transaction {@code type} (PLANNED/ACTUAL) and {@code direction} are never sent and never
 * changed by this endpoint.
 * <p>
 * Optional {@code budgetItemId}: only {@link cz.cvut.fit.budget_app.entity.Transaction.ApprovalStatus#PROPOSED}
 * or {@code DRAFT} may rebind to another budget line within the same season; {@code APPROVED} transactions
 * may only correct date, amount, and description.
 */
@Data
public class UpdateTransactionRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Size(max = 2000)
    private String description;

    /**
     * When {@code null} or equal to the current budget item, the link is unchanged.
     * When set to another id, only allowed for non-final workflow states (see service).
     */
    private Long budgetItemId;
}
