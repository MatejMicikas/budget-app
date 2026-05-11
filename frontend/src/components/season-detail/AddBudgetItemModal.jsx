import { Modal } from '../ui/Modal.jsx'
import { ITEM_TYPES } from '../../constants/domain.js'

export function AddBudgetItemModal({
  open,
  onClose,
  itemForm,
  onItemFormChange,
  onSubmit,
  fundingSources,
  teams,
  showTeamSelect,
}) {
  if (!open) return null

  return (
    <Modal title="Add budget item" onClose={onClose}>
      <form className="grid" onSubmit={onSubmit}>
        <label>
          Name
          <input
            placeholder="Name"
            value={itemForm.name}
            onChange={(e) => onItemFormChange({ ...itemForm, name: e.target.value })}
            required
          />
        </label>
        <label>
          Type
          <select value={itemForm.type} onChange={(e) => onItemFormChange({ ...itemForm, type: e.target.value })}>
            {ITEM_TYPES.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Planned amount
          <input
            placeholder="Planned amount"
            type="number"
            step="0.01"
            value={itemForm.plannedAmount}
            onChange={(e) => onItemFormChange({ ...itemForm, plannedAmount: e.target.value })}
            required
          />
        </label>
        <label>
          Funding source
          <select
            value={itemForm.fundingSourceId}
            onChange={(e) => onItemFormChange({ ...itemForm, fundingSourceId: e.target.value })}
          >
            <option value="">No funding source</option>
            {fundingSources.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        {showTeamSelect && (
          <label>
            Team (optional)
            <select value={itemForm.teamId} onChange={(e) => onItemFormChange({ ...itemForm, teamId: e.target.value })}>
              <option value="">No team</option>
              {teams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
        )}
        <button type="submit">Add item</button>
      </form>
    </Modal>
  )
}
