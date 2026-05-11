package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByNameIgnoreCase(String name);
}
