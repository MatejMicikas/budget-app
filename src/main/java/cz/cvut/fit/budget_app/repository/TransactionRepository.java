package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySeasonId(Long seasonId);

    List<Transaction> findBySeasonIdAndType(Long seasonId, Transaction.TransactionType type);

    List<Transaction> findBySeasonIdAndBudgetItemId(Long seasonId, Long budgetItemId);

    List<Transaction> findBySeasonIdAndTypeAndBudgetItemId(Long seasonId, Transaction.TransactionType type, Long budgetItemId);

    List<Transaction> findByBudgetItemId(Long budgetItemId);

    List<Transaction> findByBudgetItemFundingSourceId(Long fundingSourceId);
}
