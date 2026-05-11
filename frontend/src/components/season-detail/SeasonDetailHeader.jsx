export function SeasonDetailHeader({ season, error, info, isAdmin, onCloseSeason }) {
  return (
    <div className="season-meta-strip">
      {error && <p className="error">{error}</p>}
      {info && <p className="season-info-banner">{info}</p>}
      {season && (
        <div className="season-meta-strip-inner">
          <div>
            <strong>{season.name}</strong>
            <span className="muted">
              {season.dateFrom} — {season.dateTo}
            </span>
          </div>
          <span className={`status-pill status-${season.status}`}>{season.status}</span>
          {isAdmin && season.status === 'OPEN' && (
            <button type="button" className="btn-outline" onClick={onCloseSeason}>
              Close season
            </button>
          )}
        </div>
      )}
    </div>
  )
}
