package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.Team;
import lombok.Data;

@Data
public class TeamResponse {
    private Long id;
    private String name;

    public static TeamResponse from(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        return response;
    }
}
