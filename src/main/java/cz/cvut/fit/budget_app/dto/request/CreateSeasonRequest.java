package cz.cvut.fit.budget_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSeasonRequest {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate dateFrom;

    @NotNull
    private LocalDate dateTo;
}
