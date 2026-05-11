import { useEffect, useMemo, useState } from 'react'
import { api, toQuery } from '../api/client.js'
import { AUDIT_OPERATION_TYPES } from '../constants/audit.js'

/** `<input type="datetime-local">` returns `yyyy-MM-ddTHH:mm` without seconds; Spring often fails to bind that to LocalDateTime. */
function normalizeDatetimeLocalForApi(value) {
  if (!value) return ''
  const v = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(v)) return `${v}:00`
  return v
}

function formatTime(value) {
  if (value == null || value === '') return '—'
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value
    const pad = (n) => String(n).padStart(2, '0')
    return `${y}-${pad(mo)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(Math.floor(s))}`.slice(0, 19)
  }
  if (typeof value === 'string') return value.replace('T', ' ').slice(0, 19)
  return String(value)
}

export function AuditLogPage({ auth }) {
  const [logs, setLogs] = useState([])
  const [filter, setFilter] = useState({ operationType: '', from: '', to: '' })
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    try {
      setError('')
      const hasFrom = Boolean(filter.from)
      const hasTo = Boolean(filter.to)
      if (hasFrom !== hasTo) {
        setError('For a date range, fill in both From and To (or leave both empty).')
        return
      }
      const params = {
        operationType: filter.operationType || undefined,
        from: normalizeDatetimeLocalForApi(filter.from) || undefined,
        to: normalizeDatetimeLocalForApi(filter.to) || undefined,
      }
      setLogs(await api(`/api/audit-logs${toQuery(params)}`, { token: auth.token }))
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial list fetch
    void load()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps -- mount-only

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return logs
    return logs.filter((l) => {
      const hay = [
        l.operationType,
        l.affectedEntityType,
        String(l.affectedEntityId ?? ''),
        String(l.performedById ?? ''),
        l.performedByUsername,
        formatTime(l.timestamp),
      ]
        .join(' ')
        .toLowerCase()
      return hay.includes(q)
    })
  }, [logs, search])

  return (
    <section className="panel">
      <div className="page-toolbar">
        <h2>Audit log</h2>
        <div className="page-toolbar-actions">
          <label className="search-field">
            <span aria-hidden>⌕</span>
            <input
              type="search"
              placeholder="Search in results"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              autoComplete="off"
            />
          </label>
        </div>
      </div>
      <div className="filter-toolbar">
        <div className="filter-grid">
          <label className="filter-field">
            <span>Operation</span>
            <select value={filter.operationType} onChange={(e) => setFilter({ ...filter, operationType: e.target.value })}>
              <option value="">All operations</option>
              {AUDIT_OPERATION_TYPES.map((op) => (
                <option key={op} value={op}>
                  {op}
                </option>
              ))}
            </select>
          </label>
          <label className="filter-field">
            <span>From</span>
            <input type="datetime-local" value={filter.from} onChange={(e) => setFilter({ ...filter, from: e.target.value })} />
          </label>
          <label className="filter-field">
            <span>To</span>
            <input type="datetime-local" value={filter.to} onChange={(e) => setFilter({ ...filter, to: e.target.value })} />
          </label>
        </div>
        <div className="filter-actions">
          <button type="button" className="btn-add" onClick={load}>
            Apply filters
          </button>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Time</th>
              <th>Operation</th>
              <th>User</th>
              <th>Entity</th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.map((l) => (
              <tr key={l.id}>
                <td>{l.id}</td>
                <td>{formatTime(l.timestamp)}</td>
                <td>
                  <span className="pill">{l.operationType}</span>
                </td>
                <td>{l.performedByUsername ?? `#${l.performedById ?? '—'}`}</td>
                <td>
                  {l.affectedEntityType}#{l.affectedEntityId}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
