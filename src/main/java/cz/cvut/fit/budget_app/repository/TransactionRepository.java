package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySeasonId(Long seasonId);

    boolean existsBySeasonIdAndDateLessThan(Long seasonId, LocalDate seasonStartInclusive);

    boolean existsBySeasonIdAndDateGreaterThan(Long seasonId, LocalDate seasonEndInclusive);
    List<Transaction> findBySeasonIdAndBudgetItemTeamId(Long seasonId, Long teamId);

    List<Transaction> findBySeasonIdAndType(Long seasonId, Transaction.TransactionType type);
    List<Transaction> findBySeasonIdAndTypeAndBudgetItemTeamId(Long seasonId, Transaction.TransactionType type, Long teamId);

    List<Transaction> findBySeasonIdAndBudgetItemId(Long seasonId, Long budgetItemId);
    List<Transaction> findBySeasonIdAndBudgetItemIdAndBudgetItemTeamId(Long seasonId, Long budgetItemId, Long teamId);

    List<Transaction> findBySeasonIdAndTypeAndBudgetItemId(Long seasonId, Transaction.TransactionType type, Long budgetItemId);
    List<Transaction> findBySeasonIdAndTypeAndBudgetItemIdAndBudgetItemTeamId(Long seasonId, Transaction.TransactionType type, Long budgetItemId, Long teamId);

    List<Transaction> findByBudgetItemId(Long budgetItemId);

    boolean existsByBudgetItem_Id(Long budgetItemId);

    List<Transaction> findByBudgetItemFundingSourceId(Long fundingSourceId);
    List<Transaction> findByBudgetItemFundingSourceIdAndBudgetItemTeamId(Long fundingSourceId, Long teamId);

    boolean existsByPlannedTransaction_Id(Long plannedTransactionId);
}
