package cz.cvut.fit.budget_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(max = 255)
    private String password;
}
