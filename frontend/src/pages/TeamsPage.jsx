import { useEffect, useMemo, useState } from 'react'
import { api } from '../api/client.js'
import { CreateTeamModal } from '../components/teams/CreateTeamModal.jsx'

export function TeamsPage({ auth }) {
  const [teams, setTeams] = useState([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [teamName, setTeamName] = useState('')

  const load = async () => {
    try {
      setError('')
      setTeams(await api('/api/teams', { token: auth.token }))
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial list fetch
    void load()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps -- mount-only

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return teams
    return teams.filter((t) => t.name.toLowerCase().includes(q))
  }, [teams, search])

  const createTeam = async () => {
    await api('/api/teams', { method: 'POST', token: auth.token, body: { name: teamName } })
    setTeamName('')
    setShowCreate(false)
    await load()
  }

  return (
    <div className="grid">
      <section className="panel">
        <div className="page-toolbar">
          <h2>Teams</h2>
          <div className="page-toolbar-actions">
            <label className="search-field">
              <span aria-hidden>⌕</span>
              <input
                type="search"
                placeholder="Search teams"
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
                <th>Name</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td>{t.name}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <CreateTeamModal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        name={teamName}
        onNameChange={setTeamName}
        onSubmit={createTeam}
      />
    </div>
  )
}
