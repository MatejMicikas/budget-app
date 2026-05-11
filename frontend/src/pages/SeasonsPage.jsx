import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { CreateSeasonModal } from '../components/seasons/CreateSeasonModal.jsx'

export function SeasonsPage({ auth }) {
  const [seasons, setSeasons] = useState([])
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({ name: '', dateFrom: '', dateTo: '', memberSummaryVisible: true })

  const load = async () => {
    try {
      setError('')
      setSeasons(await api('/api/seasons', { token: auth.token }))
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
    if (!q) return seasons
    return seasons.filter((s) => s.name.toLowerCase().includes(q))
  }, [seasons, search])

  const createSeason = async () => {
    try {
      await api('/api/seasons', { method: 'POST', token: auth.token, body: form })
      setForm({ name: '', dateFrom: '', dateTo: '', memberSummaryVisible: true })
      setShowCreate(false)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <div className="grid">
      <section className="panel">
        <div className="page-toolbar">
          <h2>Seasons</h2>
          <div className="page-toolbar-actions">
            <label className="search-field">
              <span aria-hidden>⌕</span>
              <input
                type="search"
                placeholder="Search seasons"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                autoComplete="off"
              />
            </label>
            {auth.role === 'ADMIN' && (
              <button type="button" className="btn-add" onClick={() => setShowCreate(true)}>
                + ADD
              </button>
            )}
          </div>
        </div>
        {error && <p className="error">{error}</p>}
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Period</th>
                <th>Status</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.name}</td>
                  <td>
                    {s.dateFrom} - {s.dateTo}
                  </td>
                  <td>{s.status}</td>
                  <td>
                    <Link to={`/seasons/${s.id}?tab=summary`}>Open</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {auth.role === 'ADMIN' && (
        <CreateSeasonModal
          open={showCreate}
          onClose={() => setShowCreate(false)}
          form={form}
          onFormChange={setForm}
          onSubmit={createSeason}
        />
      )}
    </div>
  )
}
