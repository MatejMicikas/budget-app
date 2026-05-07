package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.FundingSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundingSourceRepository extends JpaRepository<FundingSource, Long> {
    List<FundingSource> findBySeasonId(Long seasonId);
}
