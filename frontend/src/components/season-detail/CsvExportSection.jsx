import { TRANSACTION_STATUSES } from '../../constants/domain.js'

export function CsvExportSection({ embedded, exportFilter, onExportFilterChange, onExportTransactions, onExportBudgetItems }) {
  return (
    <section className="panel flush-top">
      {!embedded && <h2>CSV export</h2>}
      <div className="filter-toolbar">
        <div className="filter-grid" style={{ maxWidth: 720 }}>
          <label className="filter-field">
            <span>Export status</span>
            <select value={exportFilter.status} onChange={(e) => onExportFilterChange({ ...exportFilter, status: e.target.value })}>
              <option value="">APPROVED only (default)</option>
              {TRANSACTION_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <div className="filter-field">
            <span>Options</span>
            <div className="row" style={{ padding: '0.35rem 0' }}>
              <input
                type="checkbox"
                id="csv-include-proposed"
                checked={exportFilter.includeProposed}
                onChange={(e) => onExportFilterChange({ ...exportFilter, includeProposed: e.target.checked })}
              />
              <label htmlFor="csv-include-proposed" style={{ margin: 0, fontWeight: 400 }}>
                Include proposed (with APPROVED)
              </label>
            </div>
          </div>
        </div>
        <div className="filter-actions">
          <button type="button" className="btn-add" onClick={onExportTransactions}>
            Export transactions
          </button>
          <button type="button" className="btn-outline" onClick={onExportBudgetItems}>
            Export budget items
          </button>
        </div>
      </div>
    </section>
  )
}
