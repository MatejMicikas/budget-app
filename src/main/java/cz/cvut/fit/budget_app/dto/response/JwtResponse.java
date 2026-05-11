package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long userId;
    private String username;
    private User.Role role;
}
