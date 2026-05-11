import { Modal } from '../ui/Modal.jsx'
import { DIRECTIONS, TRANSACTION_TYPES } from '../../constants/domain.js'

export function AddTransactionModal({
  open,
  onClose,
  transactionForm,
  onTransactionFormChange,
  onSubmit,
  budgetItems,
}) {
  if (!open) return null

  return (
    <Modal title="Add transaction" onClose={onClose}>
      <form className="grid" onSubmit={onSubmit}>
        <label>
          Date
          <input
            type="date"
            value={transactionForm.date}
            onChange={(e) => onTransactionFormChange({ ...transactionForm, date: e.target.value })}
            required
          />
        </label>
        <label>
          Amount
          <input
            type="number"
            step="0.01"
            placeholder="Amount"
            value={transactionForm.amount}
            onChange={(e) => onTransactionFormChange({ ...transactionForm, amount: e.target.value })}
            required
          />
        </label>
        <label>
          Type
          <select value={transactionForm.type} onChange={(e) => onTransactionFormChange({ ...transactionForm, type: e.target.value })}>
            {TRANSACTION_TYPES.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Direction
          <select
            value={transactionForm.direction}
            onChange={(e) => onTransactionFormChange({ ...transactionForm, direction: e.target.value })}
          >
            {DIRECTIONS.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Budget item
          <select
            value={transactionForm.budgetItemId}
            onChange={(e) => onTransactionFormChange({ ...transactionForm, budgetItemId: e.target.value })}
            required
          >
            <option value="">Budget item</option>
            {budgetItems.map((i) => (
              <option key={i.id} value={i.id}>
                {i.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Description
          <input
            placeholder="Description"
            value={transactionForm.description}
            onChange={(e) => onTransactionFormChange({ ...transactionForm, description: e.target.value })}
          />
        </label>
        <button type="submit">Add transaction</button>
      </form>
    </Modal>
  )
}
