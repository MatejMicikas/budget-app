import { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client.js'
import { ROLES } from '../constants/domain.js'
import { CreateUserModal } from '../components/users/CreateUserModal.jsx'

export function UsersPage({ auth }) {
  const [users, setUsers] = useState([])
  const [teams, setTeams] = useState([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [createForm, setCreateForm] = useState({ username: '', password: '', role: 'MEMBER' })

  const load = async () => {
    try {
      setError('')
      const [usersData, teamsData] = await Promise.all([
        api('/api/users', { token: auth.token }),
        api('/api/teams', { token: auth.token }),
      ])
      setUsers(usersData)
      setTeams(teamsData)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial list fetch
    void load()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps -- mount-only

  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return users
    return users.filter((u) => u.username.toLowerCase().includes(q))
  }, [users, search])

  const createUser = async () => {
    await api('/api/users', { method: 'POST', token: auth.token, body: createForm })
    setCreateForm({ username: '', password: '', role: 'MEMBER' })
    setShowCreate(false)
    await load()
  }

  const changeRole = async (userId, role) => {
    await api(`/api/users/${userId}/role`, { method: 'PUT', token: auth.token, body: { role } })
    await load()
  }

  const assignTeam = async (userId, teamId) => {
    if (!teamId) {
      await api(`/api/users/${userId}/team`, { method: 'DELETE', token: auth.token })
    } else {
      await api(`/api/users/${userId}/team/${teamId}`, { method: 'PUT', token: auth.token, body: {} })
    }
    await load()
  }

  return (
    <div className="grid">
      <section className="panel">
        <div className="page-toolbar">
          <h2>Users</h2>
          <div className="page-toolbar-actions">
            <label className="search-field">
              <span aria-hidden>⌕</span>
              <input
                type="search"
                placeholder="Search users"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                autoComplete="off"
              />
            </label>
            <button type="button" className="btn-add" onClick={() => setShowCreate(true)}>
              + ADD
            </button>
          </div>
        </div>
        {error && <p className="error">{error}</p>}
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Role</th>
                <th>Team</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.username}</td>
                  <td>
                    {u.role === 'ADMIN' ? (
                      <span>ADMIN</span>
                    ) : (
                      <select value={u.role} onChange={(e) => changeRole(u.id, e.target.value)}>
                        {ROLES.map((r) => (
                          <option key={r} value={r}>
                            {r}
                          </option>
                        ))}
                      </select>
                    )}
                  </td>
                  <td>
                    {u.role === 'ADMIN' ? (
                      <span>No team</span>
                    ) : (
                      <select value={u.teamId ?? ''} onChange={(e) => assignTeam(u.id, e.target.value)}>
                        <option value="">No team</option>
                        {teams.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.name}
                          </option>
                        ))}
                      </select>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <CreateUserModal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        form={createForm}
        onFormChange={setCreateForm}
        onSubmit={createUser}
      />
    </div>
  )
}
