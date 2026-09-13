import { useState } from 'react'
import { useUpdateRisk } from '../api/queries'
import type { ApiError, ClosureReason, Risk, RiskRequest, RiskStatus } from '../api/types'

const statuses: RiskStatus[] = ['OPEN', 'MITIGATING', 'CLOSED']
const statusLabels: Record<RiskStatus, string> = { OPEN: 'Open', MITIGATING: 'Mitigating', CLOSED: 'Closed' }

const closureReasons: { value: ClosureReason; label: string }[] = [
  { value: 'RISK_ACCEPTED', label: 'Risk accepted' },
  { value: 'TRANSFERRED', label: 'Transferred' },
  { value: 'NO_LONGER_APPLICABLE', label: 'No longer applicable' },
  { value: 'DUPLICATE', label: 'Duplicate' },
]

interface Props {
  risk: Risk
}

export function StatusSwitcher({ risk }: Props) {
  const updateRisk = useUpdateRisk(risk.id)
  const [closing, setClosing] = useState(false)
  const [closureReason, setClosureReason] = useState<ClosureReason | ''>('')
  const [closureJustification, setClosureJustification] = useState('')
  const [error, setError] = useState<string | null>(null)

  function baseRequest(status: RiskStatus, overrides?: Partial<RiskRequest>): RiskRequest {
    return {
      title: risk.title,
      description: risk.description ?? '',
      category: risk.category,
      owner: risk.owner,
      likelihood: risk.likelihood,
      impact: risk.impact,
      status,
      closureReason: null,
      closureJustification: null,
      ...overrides,
    }
  }

  async function selectStatus(next: RiskStatus) {
    if (next === risk.status) return
    setError(null)

    if (next === 'CLOSED' && risk.mitigationCount === 0) {
      setClosing(true)
      return
    }

    try {
      await updateRisk.mutateAsync(baseRequest(next))
    } catch (err) {
      setError((err as ApiError).detail ?? 'Failed to update status.')
    }
  }

  async function confirmClose(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await updateRisk.mutateAsync(
        baseRequest('CLOSED', { closureReason: closureReason || null, closureJustification })
      )
      setClosing(false)
      setClosureReason('')
      setClosureJustification('')
    } catch (err) {
      setError((err as ApiError).detail ?? 'Failed to close risk.')
    }
  }

  return (
    <div className="d-flex flex-column align-items-center gap-2 w-100 text-center">
      <div className="btn-group" role="group">
        {statuses.map(s => {
          const blockedOpen = s === 'OPEN' && risk.mitigationCount > 0
          const active = risk.status === s
          return (
            <button
              key={s}
              type="button"
              className={`btn btn-sm ${active ? 'btn-primary' : 'btn-outline-primary'}`}
              disabled={updateRisk.isPending || blockedOpen}
              title={blockedOpen ? 'Remove all mitigations before setting this risk back to Open' : undefined}
              onClick={() => selectStatus(s)}
            >
              {statusLabels[s]}
            </button>
          )
        })}
      </div>

      {closing && (
        <form className="card card-body text-start w-100 mt-2" onSubmit={confirmClose}>
          <p className="small text-muted mb-2">
            No mitigations are attached — explain why this risk is closing:
          </p>
          <select
            className="form-select form-select-sm mb-2"
            value={closureReason}
            onChange={e => setClosureReason(e.target.value as ClosureReason)}
            required
          >
            <option value="">— reason —</option>
            {closureReasons.map(r => (
              <option key={r.value} value={r.value}>{r.label}</option>
            ))}
          </select>
          <textarea
            className="form-control form-control-sm mb-2"
            value={closureJustification}
            onChange={e => setClosureJustification(e.target.value)}
            placeholder="Justification"
            required
          />
          <div className="d-flex justify-content-end gap-2">
            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => setClosing(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-sm btn-primary" disabled={updateRisk.isPending}>
              Confirm close
            </button>
          </div>
        </form>
      )}

      {error && <span className="text-danger small">{error}</span>}
    </div>
  )
}
