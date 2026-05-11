package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @NotNull
    private User.Role role;
}
