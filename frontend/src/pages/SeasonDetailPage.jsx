import { useEffect, useMemo, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { api, toQuery } from '../api/client.js'
import { parseOptionalAllocatedAmount } from '../utils/funding.js'
import { SeasonDetailHeader } from '../components/season-detail/SeasonDetailHeader.jsx'
import { SeasonSummaryPanel } from '../components/season-detail/SeasonSummaryPanel.jsx'
import { BudgetItemsSection } from '../components/season-detail/BudgetItemsSection.jsx'
import { FundingSourcesSection } from '../components/season-detail/FundingSourcesSection.jsx'
import { TransactionsSection } from '../components/season-detail/TransactionsSection.jsx'
import { ProposalApprovalsSection } from '../components/season-detail/ProposalApprovalsSection.jsx'
import { CsvExportSection } from '../components/season-detail/CsvExportSection.jsx'
import { AddBudgetItemModal } from '../components/season-detail/AddBudgetItemModal.jsx'
import { AddFundingSourceModal } from '../components/season-detail/AddFundingSourceModal.jsx'
import { AddTransactionModal } from '../components/season-detail/AddTransactionModal.jsx'

const SECTION_COPY = {
  summary: 'Overview',
  budget: 'Budget items',
  funding: 'Funding sources',
  transactions: 'Transactions',
  approvals: 'Pending approvals',
  export: 'CSV export',
}

export function SeasonDetailPage({ auth }) {
  const { seasonId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const [season, setSeason] = useState(null)
  const [summary, setSummary] = useState(null)
  const [items, setItems] = useState([])
  const [transactions, setTransactions] = useState([])
  const [sources, setSources] = useState([])
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [filter, setFilter] = useState({ type: '', budgetItemId: '', fundingSourceId: '' })

  const [itemForm, setItemForm] = useState({ name: '', type: 'EXPENSE', plannedAmount: '', fundingSourceId: '', teamId: '' })
  const [teams, setTeams] = useState([])
  const [transactionForm, setTransactionForm] = useState({
    date: '',
    amount: '',
    type: 'PLANNED',
    direction: 'EXPENSE',
    description: '',
    budgetItemId: '',
  })
  const [sourceForm, setSourceForm] = useState({ name: '', type: 'SPONSORSHIP', allocatedAmount: '' })
  const [exportFilter, setExportFilter] = useState({ status: '', includeProposed: false })
  const [showBudgetItemModal, setShowBudgetItemModal] = useState(false)
  const [showFundingSourceModal, setShowFundingSourceModal] = useState(false)
  const [showTransactionModal, setShowTransactionModal] = useState(false)

  const loadAll = async () => {
    try {
      setError('')
      setInfo('')
      const seasonData = await api(`/api/seasons/${seasonId}`, { token: auth.token })
      let teamsData = []
      if (auth.role === 'ADMIN') {
        try {
          teamsData = await api('/api/teams', { token: auth.token })
        } catch {
          teamsData = []
        }
      }
      let summaryData = null
      let teamLeaderMissingTeam = false
      try {
        summaryData = await api(`/api/budget-items/summary${toQuery({ seasonId })}`, { token: auth.token })
      } catch (e) {
        if (auth.role === 'TEAM_LEADER' && e.message.includes('must belong to a team')) {
          teamLeaderMissingTeam = true
          setInfo('As TEAM_LEADER you must be assigned to a team. Ask an administrator to assign you in the Users section.')
        } else {
          throw e
        }
      }

      const canLoadScopedData = auth.role !== 'MEMBER' && !teamLeaderMissingTeam
      const [itemsData, txData, sourceData] = canLoadScopedData
        ? await Promise.all([
            api(`/api/budget-items${toQuery({ seasonId })}`, { token: auth.token }),
            api(`/api/transactions${toQuery({ seasonId })}`, { token: auth.token }),
            api(`/api/funding-sources${toQuery({ seasonId })}`, { token: auth.token }),
          ])
        : [[], [], []]

      setSeason(seasonData)
      setTeams(teamsData)
      setSummary(summaryData)
      setItems(itemsData)
      setTransactions(txData)
      setSources(sourceData)
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial / season-id-scoped fetch
    void loadAll()
  }, [seasonId]) // eslint-disable-line react-hooks/exhaustive-deps -- loadAll closes over seasonId + auth

  const canEdit = ['ADMIN', 'TREASURER'].includes(auth.role)
  const canWorkTx = ['ADMIN', 'TREASURER', 'TEAM_LEADER'].includes(auth.role)
  const proposedTransactions = useMemo(
    () => transactions.filter((t) => t.status === 'PROPOSED' && t.type === 'PLANNED'),
    [transactions],
  )

  const sectionIds = useMemo(() => {
    const ids = ['summary']
    if (canEdit) {
      ids.push('budget', 'funding')
    }
    if (canWorkTx) {
      ids.push('transactions')
      if (canEdit) ids.push('approvals')
    }
    if (canEdit) ids.push('export')
    return ids
  }, [canEdit, canWorkTx])
  const tabParam = searchParams.get('tab')
  const currentSection = sectionIds.includes(tabParam) ? tabParam : sectionIds[0] ?? 'summary'

  useEffect(() => {
    const resolved = sectionIds.includes(tabParam) ? tabParam : sectionIds[0] ?? 'summary'
    if (tabParam !== resolved) {
      setSearchParams({ tab: resolved }, { replace: true })
    }
  }, [seasonId, tabParam, sectionIds, setSearchParams])

  const primaryAction = useMemo(() => {
    if (currentSection === 'budget' && canEdit) {
      return { label: '+ ADD', onClick: () => setShowBudgetItemModal(true) }
    }
    if (currentSection === 'funding' && canEdit) {
      return { label: '+ ADD', onClick: () => setShowFundingSourceModal(true) }
    }
    if (currentSection === 'transactions' && canWorkTx) {
      return { label: '+ ADD', onClick: () => setShowTransactionModal(true) }
    }
    return null
  }, [currentSection, canEdit, canWorkTx])

  const seasonAllowsEdits = season?.status === 'OPEN'
  const addButtonDisabled = Boolean(primaryAction) && !seasonAllowsEdits

  const activeTitle = SECTION_COPY[currentSection] ?? SECTION_COPY.summary

  const createBudgetItem = async (event) => {
    event.preventDefault()
    const created = await api('/api/budget-items', {
      method: 'POST',
      token: auth.token,
      body: {
        name: itemForm.name,
        type: itemForm.type,
        plannedAmount: itemForm.plannedAmount,
        seasonId: Number(seasonId),
        fundingSourceId: itemForm.fundingSourceId || null,
      },
    })
    if (auth.role === 'ADMIN' && itemForm.teamId) {
      await api(`/api/budget-items/${created.id}/team/${itemForm.teamId}`, {
        method: 'PUT',
        token: auth.token,
      })
    }
    setItemForm({ name: '', type: 'EXPENSE', plannedAmount: '', fundingSourceId: '', teamId: '' })
    setShowBudgetItemModal(false)
    await loadAll()
  }

  const assignTeamToBudgetItem = async (itemId, teamId) => {
    if (!teamId) {
      await api(`/api/budget-items/${itemId}/team`, { method: 'DELETE', token: auth.token })
    } else {
      await api(`/api/budget-items/${itemId}/team/${teamId}`, { method: 'PUT', token: auth.token })
    }
    await loadAll()
  }

  const createSource = async (event) => {
    event.preventDefault()
    await api('/api/funding-sources', {
      method: 'POST',
      token: auth.token,
      body: {
        name: sourceForm.name,
        type: sourceForm.type,
        seasonId: Number(seasonId),
        allocatedAmount: parseOptionalAllocatedAmount(sourceForm.allocatedAmount),
      },
    })
    setSourceForm({ name: '', type: 'SPONSORSHIP', allocatedAmount: '' })
    setShowFundingSourceModal(false)
    await loadAll()
  }

  const createTransaction = async (event) => {
    event.preventDefault()
    await api('/api/transactions', {
      method: 'POST',
      token: auth.token,
      body: { ...transactionForm, seasonId: Number(seasonId), budgetItemId: Number(transactionForm.budgetItemId) },
    })
    setTransactionForm({ date: '', amount: '', type: 'PLANNED', direction: 'EXPENSE', description: '', budgetItemId: '' })
    setShowTransactionModal(false)
    await loadAll()
  }

  const updateBudgetItem = async (item) => {
    const name = window.prompt('Budget item name', item.name)
    if (!name) return
    const plannedAmount = window.prompt('Planned amount', item.plannedAmount)
    if (!plannedAmount) return
    await api(`/api/budget-items/${item.id}`, {
      method: 'PUT',
      token: auth.token,
      body: {
        name,
        type: item.type,
        plannedAmount,
        fundingSourceId: item.fundingSourceId ?? null,
      },
    })
    await loadAll()
  }

  const updateSource = async (source) => {
    const name = window.prompt('Funding source name', source.name)
    if (!name) return
    const allocatedAmount = window.prompt('Allocated amount (empty = no limit)', source.allocatedAmount ?? '')
    await api(`/api/funding-sources/${source.id}`, {
      method: 'PUT',
      token: auth.token,
      body: {
        name,
        type: source.type,
        allocatedAmount: parseOptionalAllocatedAmount(allocatedAmount),
      },
    })
    await loadAll()
  }

  const updateTransaction = async (tx) => {
    const amount = window.prompt('Amount', tx.amount)
    if (!amount) return
    const description = window.prompt('Description', tx.description ?? '') ?? ''
    await api(`/api/transactions/${tx.id}`, {
      method: 'PUT',
      token: auth.token,
      body: {
        date: tx.date,
        amount,
        description,
      },
    })
    await loadAll()
  }

  const updateTransactionStatus = async (id, action) => {
    await api(`/api/transactions/${id}/${action}`, { method: 'POST', token: auth.token, body: {} })
    await loadAll()
  }

  const realizeTransaction = async (tx) => {
    const date = window.prompt('Realization date (YYYY-MM-DD)', tx.date)
    if (!date) return
    const amount = window.prompt('Realization amount', tx.amount)
    if (!amount) return
    const description = window.prompt('Realization description', `Realized from ${tx.id}`) ?? ''
    await api(`/api/transactions/${tx.id}/realize`, {
      method: 'POST',
      token: auth.token,
      body: { date, amount, description },
    })
    await loadAll()
  }

  const closeSeason = async () => {
    if (!window.confirm('Do you really want to close this season?')) return
    await api(`/api/seasons/${seasonId}/close`, { method: 'POST', token: auth.token, body: {} })
    await loadAll()
  }

  const deleteBudgetItem = async (id) => {
    await api(`/api/budget-items/${id}`, { method: 'DELETE', token: auth.token })
    await loadAll()
  }

  const deleteSource = async (id) => {
    await api(`/api/funding-sources/${id}`, { method: 'DELETE', token: auth.token })
    await loadAll()
  }

  const cancelTransaction = async (tx) => {
    const reason = window.prompt('Cancel reason (optional)') ?? ''
    await api(`/api/transactions/${tx.id}/cancel`, {
      method: 'POST',
      token: auth.token,
      body: { reason: reason.trim() || undefined },
    })
    await loadAll()
  }

  const deleteTransaction = async (tx) => {
    if (
      !window.confirm(
        `Permanently delete transaction #${tx.id} (${tx.type}, ${tx.amount})? This removes the row from the database; use Cancel if you only want to mark it as cancelled.`,
      )
    ) {
      return
    }
    try {
      setError('')
      await api(`/api/transactions/${tx.id}`, { method: 'DELETE', token: auth.token })
      await loadAll()
    } catch (e) {
      setError(e.message)
    }
  }

  const applyTransactionFilter = async () => {
    const data = await api(`/api/transactions${toQuery({ seasonId, ...filter })}`, { token: auth.token })
    setTransactions(data)
  }

  const exportCsv = async (kind) => {
    const path =
      kind === 'transactions'
        ? `/api/export/transactions/${seasonId}${toQuery(exportFilter)}`
        : `/api/export/budget-items/${seasonId}`
    const blob = await api(path, { token: auth.token, isCsv: true })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${kind}-season-${seasonId}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="season-workspace">
      <div className="season-main">
        <SeasonDetailHeader
          season={season}
          error={error}
          info={info}
          isAdmin={auth.role === 'ADMIN'}
          onCloseSeason={closeSeason}
        />

        <div className="workspace-toolbar">
          <div>
            <h1>{activeTitle}</h1>
            {season && (
              <p className="muted" style={{ margin: '0.25rem 0 0' }}>
                {season.name} · {season.dateFrom} — {season.dateTo}
              </p>
            )}
          </div>
          <div className="workspace-toolbar-right">
            {primaryAction && (
              <button
                type="button"
                className="btn-add"
                disabled={addButtonDisabled}
                title={addButtonDisabled ? 'Season is closed — you cannot add records.' : undefined}
                onClick={primaryAction.onClick}
              >
                {primaryAction.label}
              </button>
            )}
          </div>
        </div>

        <div className="workspace-body">
          {currentSection === 'summary' && (
            <div className="grid">
              <SeasonSummaryPanel summary={summary} />
            </div>
          )}

          {currentSection === 'budget' && canEdit && (
            <BudgetItemsSection
              embedded
              items={items}
              teams={teams}
              role={auth.role}
              onAddClick={() => setShowBudgetItemModal(true)}
              onAssignTeam={assignTeamToBudgetItem}
              onEditItem={updateBudgetItem}
              onDeleteItem={deleteBudgetItem}
            />
          )}

          {currentSection === 'funding' && canEdit && (
            <FundingSourcesSection
              embedded
              sources={sources}
              onAddClick={() => setShowFundingSourceModal(true)}
              onEditSource={updateSource}
              onDeleteSource={deleteSource}
            />
          )}

          {currentSection === 'transactions' && canWorkTx && (
            <TransactionsSection
              embedded
              transactions={transactions}
              items={items}
              sources={sources}
              filter={filter}
              onFilterChange={setFilter}
              canEdit={canEdit}
              onApplyFilter={applyTransactionFilter}
              onResetFilters={loadAll}
              onUpdateTransaction={updateTransaction}
              onRealizeTransaction={realizeTransaction}
              onCancelTransaction={cancelTransaction}
              onDeleteTransaction={deleteTransaction}
            />
          )}

          {currentSection === 'approvals' && canEdit && canWorkTx && (
            <ProposalApprovalsSection
              embedded
              proposedTransactions={proposedTransactions}
              onApprove={(id) => updateTransactionStatus(id, 'approve')}
              onReject={(id) => updateTransactionStatus(id, 'reject')}
            />
          )}

          {currentSection === 'export' && canEdit && (
            <CsvExportSection
              embedded
              exportFilter={exportFilter}
              onExportFilterChange={setExportFilter}
              onExportTransactions={() => exportCsv('transactions')}
              onExportBudgetItems={() => exportCsv('budget-items')}
            />
          )}
        </div>
      </div>

      <AddBudgetItemModal
        open={showBudgetItemModal}
        onClose={() => setShowBudgetItemModal(false)}
        itemForm={itemForm}
        onItemFormChange={setItemForm}
        onSubmit={createBudgetItem}
        fundingSources={sources}
        teams={teams}
        showTeamSelect={auth.role === 'ADMIN' && teams.length > 0}
      />

      <AddFundingSourceModal
        open={showFundingSourceModal}
        onClose={() => setShowFundingSourceModal(false)}
        sourceForm={sourceForm}
        onSourceFormChange={setSourceForm}
        onSubmit={createSource}
      />

      <AddTransactionModal
        open={showTransactionModal}
        onClose={() => setShowTransactionModal(false)}
        transactionForm={transactionForm}
        onTransactionFormChange={setTransactionForm}
        onSubmit={createTransaction}
        budgetItems={items}
      />
    </div>
  )
}
