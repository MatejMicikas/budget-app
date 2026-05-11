package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    List<BudgetItem> findBySeasonId(Long seasonId);
    List<BudgetItem> findBySeasonIdAndTeamId(Long seasonId, Long teamId);
    boolean existsByIdAndTeamId(Long id, Long teamId);
    List<BudgetItem> findByFundingSourceId(Long fundingSourceId);
    boolean existsByFundingSourceIdAndTeamId(Long fundingSourceId, Long teamId);
    boolean existsBySeasonId(Long seasonId);

    long countByTeam_Id(Long teamId);
}
