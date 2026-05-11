export function FundingSourcesSection({ embedded, sources, onAddClick, onEditSource, onDeleteSource }) {
  return (
    <section className="panel flush-top">
      {!embedded && (
        <div className="section-head">
          <h2>Funding sources</h2>
          <button type="button" className="btn-add" onClick={onAddClick}>
            + Add source
          </button>
        </div>
      )}
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Type</th>
              <th>Allocated</th>
              <th>Actual</th>
              <th>Over limit</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {sources.map((s) => (
              <tr key={s.id}>
                <td>
                  <span className={`status-dot ${s.limitExceeded ? 'bad' : 'ok'}`} />
                  {s.id}
                </td>
                <td>{s.name}</td>
                <td>
                  <span className="pill">{s.type}</span>
                </td>
                <td>{s.allocatedAmount ?? '—'}</td>
                <td>{s.actualSpending}</td>
                <td>{s.limitExceeded ? <span className="pill pill-accent">Yes</span> : <span className="pill">No</span>}</td>
                <td>
                  <div className="cell-actions">
                    <button type="button" className="btn-edit" onClick={() => onEditSource(s)}>
                      Edit
                    </button>
                    <button type="button" className="btn-danger" onClick={() => onDeleteSource(s.id)}>
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
