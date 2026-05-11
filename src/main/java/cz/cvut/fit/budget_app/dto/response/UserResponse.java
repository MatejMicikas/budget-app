package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private User.Role role;
    private Long teamId;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.role = user.getRole();
        r.teamId = user.getTeam() != null ? user.getTeam().getId() : null;
        return r;
    }
}
