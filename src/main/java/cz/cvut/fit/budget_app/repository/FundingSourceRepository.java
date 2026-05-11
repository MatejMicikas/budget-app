package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.FundingSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FundingSourceRepository extends JpaRepository<FundingSource, Long> {
    List<FundingSource> findBySeasonId(Long seasonId);

    @Query("""
            select distinct fs
            from FundingSource fs
            join BudgetItem bi on bi.fundingSource.id = fs.id
            where fs.season.id = :seasonId
              and bi.team.id = :teamId
            """)
    List<FundingSource> findBySeasonIdAndTeamId(Long seasonId, Long teamId);
}
