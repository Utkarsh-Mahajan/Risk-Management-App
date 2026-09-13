import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useRisk, useDeleteRisk, useCreateMitigation, useDeleteMitigation } from '../api/queries'
import { SeverityBadge } from '../components/SeverityBadge'
import { ClosureBanner } from '../components/ClosureBanner'
import { StatusSwitcher } from '../components/StatusSwitcher'
import { SelectDropdown } from '../components/SelectDropdown'
import type { ApiError } from '../api/types'

export function RiskDetailPage() {
  const { id } = useParams<{ id: string }>()
  const riskId = Number(id)
  const navigate = useNavigate()

  const { data: risk, isLoading } = useRisk(riskId)
  const deleteRisk = useDeleteRisk()
  const createMitigation = useCreateMitigation(riskId)
  const deleteMitigation = useDeleteMitigation(riskId)

  const [mitigDesc, setMitigDesc] = useState('')
  const [mitigEff, setMitigEff] = useState(1)
  const [mitigError, setMitigError] = useState<string | null>(null)

  async function handleAddMitigation(e: React.FormEvent) {
    e.preventDefault()
    setMitigError(null)
    try {
      await createMitigation.mutateAsync({ description: mitigDesc, effectiveness: mitigEff })
      setMitigDesc('')
      setMitigEff(1)
    } catch (err) {
      setMitigError((err as ApiError).detail ?? 'Failed to add mitigation.')
    }
  }

  async function handleDeleteMitigation(mitigationId: number) {
    try {
      await deleteMitigation.mutateAsync(mitigationId)
    } catch (err) {
      alert((err as ApiError).detail ?? 'Cannot delete mitigation.')
    }
  }

  async function handleDeleteRisk() {
    if (!confirm('Delete this risk? This cannot be undone.')) return
    await deleteRisk.mutateAsync(riskId)
    navigate('/')
  }

  if (isLoading) return <div className="container py-4">Loading…</div>
  if (!risk) return <div className="container py-4">Risk not found.</div>

  return (
    <div className="container pb-5" style={{ maxWidth: 1100 }}>
      <div className="d-flex align-items-start justify-content-between gap-3 mb-4">
        <div>
          <button className="btn btn-link p-0 mb-2 text-decoration-none" onClick={() => navigate('/')}>
            ← All risks
          </button>
          <h1 className="h3 mb-1">{risk.title}</h1>
          <div className="text-muted small d-flex gap-2">
            <span>{risk.category.charAt(0) + risk.category.slice(1).toLowerCase()}</span>
            <span>·</span>
            <span>{risk.owner}</span>
            <span>·</span>
            <span>Likelihood {risk.likelihood} × Impact {risk.impact}</span>
          </div>
        </div>
        <div className="d-flex gap-2 flex-shrink-0">
          <button className="btn btn-outline-secondary" onClick={() => navigate(`/risks/${riskId}/edit`)}>
            Edit
          </button>
          <button className="btn btn-danger" onClick={handleDeleteRisk}>
            Delete
          </button>
        </div>
      </div>

      <div className="row g-3 mb-4 text-center">
        <div className="col-6 col-md-3">
          <div className="card h-100 shadow-sm">
            <div className="card-body d-flex flex-column align-items-center">
              <div className="text-uppercase small text-muted mb-1">Inherent</div>
              <div className="d-flex flex-column align-items-center justify-content-center flex-grow-1">
                <div className="fs-3 fw-bold mb-2">{risk.inherentScore}</div>
                <SeverityBadge band={risk.inherentBand} />
              </div>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card h-100 shadow-sm">
            <div className="card-body d-flex flex-column align-items-center">
              <div className="text-uppercase small text-muted mb-1">Residual</div>
              <div className="d-flex flex-column align-items-center justify-content-center flex-grow-1">
                <div className="fs-3 fw-bold mb-2">{risk.residualScore}</div>
                <SeverityBadge band={risk.residualBand} />
              </div>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card h-100 shadow-sm">
            <div className="card-body d-flex flex-column align-items-center">
              <div className="text-uppercase small text-muted mb-1">Controls</div>
              <div className="d-flex align-items-center justify-content-center flex-grow-1">
                <div className="fs-3 fw-bold">{risk.mitigationCount}</div>
              </div>
            </div>
          </div>
        </div>
        <div className="col-6 col-md-3">
          <div className="card h-100 shadow-sm">
            <div className="card-body d-flex flex-column align-items-center">
              <div className="text-uppercase small text-muted mb-2">Status</div>
              <div className="d-flex align-items-center justify-content-center flex-grow-1 w-100">
                <StatusSwitcher risk={risk} />
              </div>
            </div>
          </div>
        </div>
      </div>

      <ClosureBanner risk={risk} />

      {risk.description && (
        <div className="card shadow-sm mb-4">
          <div className="card-body">
            <h2 className="h6 text-uppercase text-muted mb-2">Description</h2>
            <p className="mb-0" style={{ whiteSpace: 'pre-wrap' }}>{risk.description}</p>
          </div>
        </div>
      )}

      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <h2 className="h6 text-uppercase text-muted mb-3">Mitigating Controls</h2>

          {risk.mitigations.length === 0 && (
            <p className="text-muted small">No controls attached yet.</p>
          )}

          {risk.mitigations.length > 0 && (
            <ul className="list-group list-group-flush mb-3">
              {risk.mitigations.map(m => (
                <li key={m.id} className="list-group-item d-flex align-items-center gap-3 px-0">
                  <span className="flex-grow-1">{m.description}</span>
                  <span className="badge text-bg-primary fw-normal">Eff {m.effectiveness}/5</span>
                  <button
                    type="button"
                    className="btn-close-icon"
                    onClick={() => handleDeleteMitigation(m.id)}
                    title="Remove control"
                    aria-label="Remove control"
                  >
                    ✕
                  </button>
                </li>
              ))}
            </ul>
          )}

          <form onSubmit={handleAddMitigation}>
            <div className="d-flex gap-2 flex-nowrap align-items-center">
              <input
                className="form-control flex-grow-1"
                style={{ minWidth: 0 }}
                placeholder="Describe the control…"
                value={mitigDesc}
                onChange={e => setMitigDesc(e.target.value)}
                required
              />
              <div className="flex-shrink-0">
                <SelectDropdown
                  value={mitigEff}
                  options={[1, 2, 3, 4, 5].map(v => ({ value: v, label: `Eff ${v}` }))}
                  onChange={setMitigEff}
                />
              </div>
              <button
                type="submit"
                className="btn btn-primary flex-shrink-0"
                disabled={createMitigation.isPending}
              >
                Add
              </button>
            </div>
            {mitigError && <div className="text-danger small mt-2">{mitigError}</div>}
          </form>
        </div>
      </div>
    </div>
  )
}
