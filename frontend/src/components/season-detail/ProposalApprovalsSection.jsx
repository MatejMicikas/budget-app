export function ProposalApprovalsSection({ embedded, proposedTransactions, onApprove, onReject }) {
  return (
    <section className="panel flush-top">
      {!embedded && <h2>Proposal approvals</h2>}
      <div style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date</th>
              <th>Amount</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {proposedTransactions.map((t) => (
              <tr key={t.id}>
                <td>
                  <span className="status-dot warn" />
                  {t.id}
                </td>
                <td>{t.date}</td>
                <td>{t.amount}</td>
                <td>{t.description || '—'}</td>
                <td>
                  <div className="cell-actions">
                    <button type="button" className="btn-add" onClick={() => onApprove(t.id)}>
                      Approve
                    </button>
                    <button type="button" className="btn-danger" onClick={() => onReject(t.id)}>
                      Reject
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
