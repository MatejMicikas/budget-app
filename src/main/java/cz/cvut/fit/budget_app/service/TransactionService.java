package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetItemService budgetItemService;
    private final SeasonService seasonService;
    private final FundingSourceService fundingSourceService;
    private final AuditLogService auditLogService;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request, Long performedByUserId) {
        BudgetItem budgetItem = budgetItemService.getOrThrow(request.getBudgetItemId());
        if (!budgetItem.getSeason().getId().equals(request.getSeasonId())) {
            throw new IllegalArgumentException(
                    "Transaction seasonId does not match BudgetItem season");
        }
        seasonService.requireOpen(budgetItem.getSeason());

        // direction must match budget item type
        Transaction.Direction expectedDirection = budgetItem.getType() == BudgetItem.ItemType.INCOME
                ? Transaction.Direction.INCOME
                : Transaction.Direction.EXPENSE;
        if (request.getDirection() != expectedDirection) {
            throw new IllegalArgumentException(
                    "Transaction direction " + request.getDirection()
                            + " does not match BudgetItem type " + budgetItem.getType());
        }

        Transaction tx = new Transaction();
        tx.setDate(request.getDate());
        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setDirection(request.getDirection());
        tx.setDescription(request.getDescription());
        tx.setBudgetItem(budgetItem);
        tx.setSeason(budgetItem.getSeason());

        Transaction saved = transactionRepository.save(tx);

        // For purpose-bound sources, reject new states that exceed allocated limit.
        if (budgetItem.getFundingSource() != null) {
            fundingSourceService.validateSpendingLimitOrThrow(budgetItem.getFundingSource().getId());
        }

        auditLogService.logTransactionCreated(saved.getId(), performedByUserId);

        return TransactionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return TransactionResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findBySeasonId(Long seasonId) {
        return transactionRepository.findBySeasonId(seasonId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findBySeasonWithFilters(Long seasonId,
                                                             Transaction.TransactionType type,
                                                             Long budgetItemId) {
        List<Transaction> transactions;
        if (type != null && budgetItemId != null) {
            transactions = transactionRepository.findBySeasonIdAndTypeAndBudgetItemId(seasonId, type, budgetItemId);
        } else if (type != null) {
            transactions = transactionRepository.findBySeasonIdAndType(seasonId, type);
        } else if (budgetItemId != null) {
            transactions = transactionRepository.findBySeasonIdAndBudgetItemId(seasonId, budgetItemId);
        } else {
            transactions = transactionRepository.findBySeasonId(seasonId);
        }

        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByBudgetItemId(Long budgetItemId) {
        return transactionRepository.findByBudgetItemId(budgetItemId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByFundingSourceId(Long fundingSourceId) {
        return transactionRepository.findByBudgetItemFundingSourceId(fundingSourceId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());

        transactionRepository.delete(tx);
        auditLogService.logTransactionDeleted(id, performedByUserId);
    }

    private Transaction getOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }
}
