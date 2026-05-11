export function BudgetItemsSection({
  embedded,
  items,
  teams,
  role,
  onAddClick,
  onAssignTeam,
  onEditItem,
  onDeleteItem,
}) {
  return (
    <section className="panel flush-top">
      {!embedded && (
        <div className="section-head">
          <h2>Budget items</h2>
          <button type="button" className="btn-add" onClick={onAddClick}>
            + Add item
          </button>
        </div>
      )}
      {role === 'ADMIN' && (
        <p className="muted">Assign each line item to a team so team leaders can propose transactions for it.</p>
      )}
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Type</th>
              <th>Planned</th>
              <th>Actual</th>
              <th>Balance</th>
              <th>Team</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.id}>
                <td>
                  <span className="status-dot ok" /> {i.id}
                </td>
                <td>{i.name}</td>
                <td>
                  <span className="pill">{i.type}</span>
                </td>
                <td>{i.plannedAmount}</td>
                <td>{i.actualAmount}</td>
                <td>{i.balance}</td>
                <td>
                  {role === 'ADMIN' && teams.length > 0 ? (
                    <select className="table-select" value={i.teamId ?? ''} onChange={(e) => onAssignTeam(i.id, e.target.value)}>
                      <option value="">No team</option>
                      {teams.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.name}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className="muted">{i.teamId != null ? `#${i.teamId}` : '—'}</span>
                  )}
                </td>
                <td>
                  <div className="cell-actions">
                    <button type="button" className="btn-edit" onClick={() => onEditItem(i)}>
                      Edit
                    </button>
                    <button type="button" className="btn-danger" onClick={() => onDeleteItem(i.id)}>
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
