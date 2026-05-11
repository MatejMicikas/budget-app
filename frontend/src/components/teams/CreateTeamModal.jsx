import { Modal } from '../ui/Modal.jsx'

export function CreateTeamModal({ open, onClose, name, onNameChange, onSubmit }) {
  if (!open) return null

  return (
    <Modal title="Create team" onClose={onClose}>
      <form
        className="grid"
        onSubmit={(e) => {
          e.preventDefault()
          onSubmit(e)
        }}
      >
        <label>
          Team name
          <input value={name} onChange={(e) => onNameChange(e.target.value)} required />
        </label>
        <button type="submit">Create team</button>
      </form>
    </Modal>
  )
}
