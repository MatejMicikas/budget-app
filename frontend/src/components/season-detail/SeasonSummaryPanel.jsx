export function SeasonSummaryPanel({ summary }) {
  if (!summary) return null

  return (
    <section className="panel flush-top">
      <div className="section-head">
        <h2>Balance and summary</h2>
      </div>
      <div className="summary-kpi-row">
        <div className="summary-kpi">
          <div className="lbl">Planned</div>
          <div className="val">{summary.totalPlannedAmount}</div>
        </div>
        <div className="summary-kpi">
          <div className="lbl">Actual / Tx</div>
          <div className="val">{summary.totalTransactionAmount}</div>
        </div>
        <div className="summary-kpi">
          <div className="lbl">Remaining</div>
          <div className="val">{summary.totalRemainingAmount}</div>
        </div>
        <div className="summary-kpi">
          <div className="lbl">Variance</div>
          <div className="val">{summary.totalVarianceAmount}</div>
        </div>
      </div>
    </section>
  )
}
