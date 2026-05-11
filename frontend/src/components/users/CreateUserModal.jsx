import { Modal } from '../ui/Modal.jsx'
import { ROLES } from '../../constants/domain.js'

export function CreateUserModal({ open, onClose, form, onFormChange, onSubmit }) {
  if (!open) return null

  return (
    <Modal title="Create user" onClose={onClose}>
      <form
        className="grid"
        onSubmit={(e) => {
          e.preventDefault()
          onSubmit(e)
        }}
      >
        <label>
          Username
          <input value={form.username} onChange={(e) => onFormChange({ ...form, username: e.target.value })} required />
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={(e) => onFormChange({ ...form, password: e.target.value })}
            required
          />
        </label>
        <label>
          Role
          <select value={form.role} onChange={(e) => onFormChange({ ...form, role: e.target.value })}>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Create</button>
      </form>
    </Modal>
  )
}
