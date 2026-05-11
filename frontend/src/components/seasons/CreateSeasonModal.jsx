import { Modal } from '../ui/Modal.jsx'

export function CreateSeasonModal({ open, onClose, form, onFormChange, onSubmit }) {
  if (!open) return null

  return (
    <Modal title="Create season" onClose={onClose}>
      <form
        className="grid"
        onSubmit={(e) => {
          e.preventDefault()
          onSubmit(e)
        }}
      >
        <label>
          Name
          <input value={form.name} onChange={(e) => onFormChange({ ...form, name: e.target.value })} required />
        </label>
        <label>
          From
          <input type="date" value={form.dateFrom} onChange={(e) => onFormChange({ ...form, dateFrom: e.target.value })} required />
        </label>
        <label>
          To
          <input type="date" value={form.dateTo} onChange={(e) => onFormChange({ ...form, dateTo: e.target.value })} required />
        </label>
        <label className="row">
          <input
            type="checkbox"
            checked={form.memberSummaryVisible}
            onChange={(e) => onFormChange({ ...form, memberSummaryVisible: e.target.checked })}
          />{' '}
          Enable MEMBER summary
        </label>
        <button type="submit">Create</button>
      </form>
    </Modal>
  )
}
