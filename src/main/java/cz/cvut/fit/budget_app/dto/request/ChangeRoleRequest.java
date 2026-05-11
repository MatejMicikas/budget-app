package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull
    private User.Role role;
}
