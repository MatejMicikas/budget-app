package cz.cvut.fit.budget_app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelTransactionRequest {

    @Size(max = 2000)
    private String reason;
}
