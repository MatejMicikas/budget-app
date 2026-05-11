import { Modal } from '../ui/Modal.jsx'
import { FUNDING_TYPES } from '../../constants/domain.js'

export function AddFundingSourceModal({ open, onClose, sourceForm, onSourceFormChange, onSubmit }) {
  if (!open) return null

  return (
    <Modal title="Add funding source" onClose={onClose}>
      <form className="grid" onSubmit={onSubmit}>
        <label>
          Name
          <input
            placeholder="Name"
            value={sourceForm.name}
            onChange={(e) => onSourceFormChange({ ...sourceForm, name: e.target.value })}
            required
          />
        </label>
        <label>
          Type
          <select value={sourceForm.type} onChange={(e) => onSourceFormChange({ ...sourceForm, type: e.target.value })}>
            {FUNDING_TYPES.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>
        </label>
        <label>
          Allocated amount (optional)
          <input
            placeholder="Leave empty for no limit"
            type="number"
            step="0.01"
            min="0"
            value={sourceForm.allocatedAmount}
            onChange={(e) => onSourceFormChange({ ...sourceForm, allocatedAmount: e.target.value })}
          />
        </label>
        <button type="submit">Add source</button>
      </form>
    </Modal>
  )
}
