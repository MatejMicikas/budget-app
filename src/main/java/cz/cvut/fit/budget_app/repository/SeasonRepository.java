package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    Optional<Season> findFirstByStatus(Season.SeasonStatus status);
    List<Season> findAllByStatus(Season.SeasonStatus status);
}
