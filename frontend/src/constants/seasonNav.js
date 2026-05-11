/** Tabs under a season detail (`?tab=`); visibility mirrors SeasonDetailPage sections. */
export function getSeasonTabsForRole(role) {
  const canEdit = ['ADMIN', 'TREASURER'].includes(role)
  const canWorkTx = ['ADMIN', 'TREASURER', 'TEAM_LEADER'].includes(role)
  const tabs = [{ id: 'summary', label: 'Overview' }]
  if (canEdit) {
    tabs.push({ id: 'budget', label: 'Budget items' }, { id: 'funding', label: 'Funding sources' })
  }
  if (canWorkTx) {
    tabs.push({ id: 'transactions', label: 'Transactions' })
    if (canEdit) tabs.push({ id: 'approvals', label: 'Approvals' })
  }
  if (canEdit) tabs.push({ id: 'export', label: 'CSV export' })
  return tabs
}
