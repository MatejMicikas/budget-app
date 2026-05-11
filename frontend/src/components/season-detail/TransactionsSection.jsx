import { useMemo, useState } from 'react'
import { TRANSACTION_TYPES } from '../../constants/domain.js'

function statusDotClass(status) {
  if (status === 'APPROVED' || status === 'PROPOSED') return 'ok'
  if (status === 'CANCELLED' || status === 'REJECTED') return 'bad'
  return 'warn'
}

const COL_LABELS = {
  id: 'ID',
  date: 'Date',
  type: 'Type',
  status: 'Status',
  amount: 'Amount',
  warning: 'Warning',
  actions: 'Actions',
}

export function TransactionsSection({
  embedded,
  transactions,
  items,
  sources,
  filter,
  onFilterChange,
  canEdit,
  onAddTransaction,
  onApplyFilter,
  onResetFilters,
  onUpdateTransaction,
  onRealizeTransaction,
  onCancelTransaction,
  onDeleteTransaction,
}) {
  const [quickSearch, setQuickSearch] = useState('')
  const [filtersVisible, setFiltersVisible] = useState(true)
  const [columnPickerOpen, setColumnPickerOpen] = useState(false)
  const [cols, setCols] = useState({
    id: true,
    date: true,
    type: true,
    status: true,
    amount: true,
    warning: true,
    actions: true,
  })

  const visibleTransactions = useMemo(() => {
    const q = quickSearch.trim().toLowerCase()
    if (!q) return transactions
    return transactions.filter((t) => {
      const blob = [String(t.id), t.date, t.type, t.direction, t.status, String(t.amount), t.description ?? '', t.fundingLimitWarningMessage ?? '']
        .join(' ')
        .toLowerCase()
      return blob.includes(q)
    })
  }, [transactions, quickSearch])

  const toggleCol = (key) => {
    setCols((c) => {
      const next = { ...c, [key]: !c[key] }
      if (!Object.values(next).some(Boolean)) return c
      return next
    })
  }

  return (
    <section className="panel flush-top">
      {!embedded && (
        <div className="section-head">
          <h2>Transactions</h2>
          <button type="button" className="btn-add" onClick={onAddTransaction}>
            + Add transaction
          </button>
        </div>
      )}

      <div className="page-toolbar-actions" style={{ marginBottom: '0.65rem', justifyContent: 'flex-end', width: '100%' }}>
        <label className="search-field">
          <span aria-hidden>⌕</span>
          <input
            type="search"
            placeholder="Search in loaded transactions"
            value={quickSearch}
            onChange={(e) => setQuickSearch(e.target.value)}
            autoComplete="off"
          />
        </label>
      </div>

      <div className="filter-toolbar">
        <div className="filter-toolbar-icons">
          <button
            type="button"
            className="btn-icon"
            aria-expanded={filtersVisible}
            aria-label={filtersVisible ? 'Hide filters' : 'Show filters'}
            title={filtersVisible ? 'Hide filter row' : 'Show filter row'}
            onClick={() => setFiltersVisible((v) => !v)}
          >
            ▼
          </button>
          <div className="column-picker-wrap">
            <button
              type="button"
              className="btn-icon"
              aria-expanded={columnPickerOpen}
              aria-haspopup="true"
              aria-label="Choose visible columns"
              title="Choose visible columns"
              onClick={() => setColumnPickerOpen((o) => !o)}
            >
              ☰
            </button>
            {columnPickerOpen && (
              <div className="column-picker-popover" role="dialog" aria-label="Columns">
                {Object.keys(COL_LABELS).map((key) => (
                  <label key={key} className="column-picker-row">
                    <input type="checkbox" checked={cols[key]} onChange={() => toggleCol(key)} />
                    {COL_LABELS[key]}
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>
        {filtersVisible && (
          <>
            <div className="filter-grid">
              <label className="filter-field">
                <span>Type</span>
                <select value={filter.type} onChange={(e) => onFilterChange({ ...filter, type: e.target.value })}>
                  <option value="">All types</option>
                  {TRANSACTION_TYPES.map((v) => (
                    <option key={v} value={v}>
                      {v}
                    </option>
                  ))}
                </select>
              </label>
              <label className="filter-field">
                <span>Budget item</span>
                <select value={filter.budgetItemId} onChange={(e) => onFilterChange({ ...filter, budgetItemId: e.target.value })}>
                  <option value="">All items</option>
                  {items.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="filter-field">
                <span>Funding source</span>
                <select
                  value={filter.fundingSourceId}
                  onChange={(e) => onFilterChange({ ...filter, fundingSourceId: e.target.value })}
                >
                  <option value="">All sources</option>
                  {sources.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="filter-actions">
              <button type="button" onClick={onApplyFilter}>
                Apply
              </button>
              <button type="button" className="btn-outline" onClick={onResetFilters}>
                Reset
              </button>
            </div>
          </>
        )}
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              {cols.id && <th>ID</th>}
              {cols.date && <th>Date</th>}
              {cols.type && <th>Type</th>}
              {cols.status && <th>Status</th>}
              {cols.amount && <th>Amount</th>}
              {cols.warning && <th>Warning</th>}
              {cols.actions && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {visibleTransactions.map((t) => (
              <tr key={t.id}>
                {cols.id && (
                  <td>
                    <span className={`status-dot ${statusDotClass(t.status)}`} title={t.status} />
                    {t.id}
                  </td>
                )}
                {cols.date && <td>{t.date}</td>}
                {cols.type && (
                  <td>
                    <span className="pill">
                      {t.type}/{t.direction}
                    </span>
                  </td>
                )}
                {cols.status && (
                  <td>
                    <span className="pill pill-accent">{t.status}</span>
                  </td>
                )}
                {cols.amount && <td>{t.amount}</td>}
                {cols.warning && (
                  <td>{t.fundingLimitWarningMessage ? <span className="pill">{t.fundingLimitWarningMessage}</span> : '—'}</td>
                )}
                {cols.actions && (
                  <td>
                    <div className="cell-actions">
                      {canEdit && t.status !== 'CANCELLED' && t.status !== 'REJECTED' && (
                        <>
                          <button type="button" className="btn-edit" onClick={() => onUpdateTransaction(t)}>
                            Edit
                          </button>
                          {t.type === 'PLANNED' && t.status === 'APPROVED' && (
                            <button type="button" onClick={() => onRealizeTransaction(t)}>
                              Realize
                            </button>
                          )}
                          <button type="button" className="btn-outline" onClick={() => onCancelTransaction(t)}>
                            Cancel
                          </button>
                        </>
                      )}
                      {canEdit && (
                        <button type="button" className="btn-danger" onClick={() => onDeleteTransaction(t)}>
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
