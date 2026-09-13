import { useState } from 'react'
import { useUpdateRisk } from '../api/queries'
import { Modal } from './Modal'
import { SelectDropdown } from './SelectDropdown'
import type { ApiError, ClosureReason, Risk, RiskRequest, RiskStatus } from '../api/types'

const statuses: RiskStatus[] = ['OPEN', 'MITIGATING', 'CLOSED']
const statusLabels: Record<RiskStatus, string> = { OPEN: 'Open', MITIGATING: 'Mitigating', CLOSED: 'Closed' }

const closureReasonOptions: { value: ClosureReason | ''; label: string }[] = [
  { value: '', label: '— reason —' },
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

  function closeModal() {
    setClosing(false)
    setClosureReason('')
    setClosureJustification('')
    setError(null)
  }

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
    if (!closureReason) {
      setError('Please select a closure reason.')
      return
    }
    try {
      await updateRisk.mutateAsync(
        baseRequest('CLOSED', { closureReason: closureReason || null, closureJustification })
      )
      closeModal()
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
        <Modal title="Close risk with no mitigations" onClose={closeModal}>
          <form onSubmit={confirmClose}>
            <div className="modal-body">
              <p className="small text-muted mb-3">
                No mitigations are attached — explain why this risk is closing:
              </p>
              <SelectDropdown
                className="mb-2 w-100"
                value={closureReason}
                options={closureReasonOptions}
                onChange={setClosureReason}
              />
              <textarea
                className="form-control"
                value={closureJustification}
                onChange={e => setClosureJustification(e.target.value)}
                placeholder="Justification"
                rows={3}
                required
              />
              {error && <div className="text-danger small mt-2">{error}</div>}
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={closeModal}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={updateRisk.isPending}>
                Confirm close
              </button>
            </div>
          </form>
        </Modal>
      )}

      {!closing && error && <span className="text-danger small">{error}</span>}
    </div>
  )
}
