import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { ClosureReason, RiskCategory, RiskStatus } from '../api/types'
import { useRisk, useCreateRisk, useUpdateRisk } from '../api/queries'
import { SeverityBadge } from '../components/SeverityBadge'
import { SelectDropdown } from '../components/SelectDropdown'
import type { ApiError } from '../api/types'

const categories: RiskCategory[] = ['OPERATIONAL', 'FINANCIAL', 'COMPLIANCE', 'SECURITY', 'STRATEGIC']
const statuses: RiskStatus[] = ['OPEN', 'MITIGATING', 'CLOSED']
const closureReasons: ClosureReason[] = ['MITIGATED', 'RISK_ACCEPTED', 'TRANSFERRED', 'NO_LONGER_APPLICABLE', 'DUPLICATE']
const scoreOptions = [1, 2, 3, 4, 5].map(n => ({ value: n, label: String(n) }))

function severityBand(score: number) {
  if (score >= 20) return 'CRITICAL' as const
  if (score >= 13) return 'HIGH' as const
  if (score >= 6) return 'MEDIUM' as const
  return 'LOW' as const
}

export function RiskFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = !!id
  const navigate = useNavigate()

  const { data: existing } = useRisk(Number(id))
  const createRisk = useCreateRisk()
  const updateRisk = useUpdateRisk(Number(id))

  const [title, setTitle] = useState(existing?.title ?? '')
  const [description, setDescription] = useState(existing?.description ?? '')
  const [category, setCategory] = useState<RiskCategory>(existing?.category ?? 'OPERATIONAL')
  const [owner, setOwner] = useState(existing?.owner ?? '')
  const [likelihood, setLikelihood] = useState(existing?.likelihood ?? 3)
  const [impact, setImpact] = useState(existing?.impact ?? 3)
  const [status, setStatus] = useState<RiskStatus>(existing?.status ?? 'OPEN')
  const [closureReason, setClosureReason] = useState<ClosureReason | ''>(existing?.closureReason ?? '')
  const [closureJustification, setClosureJustification] = useState(existing?.closureJustification ?? '')
  const [apiError, setApiError] = useState<string | null>(null)

  const inherent = likelihood * impact
  const band = severityBand(inherent)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setApiError(null)

    const body = {
      title,
      description,
      category,
      owner,
      likelihood,
      impact,
      ...(isEdit
        ? {
            status,
            closureReason: status === 'CLOSED' && closureReason ? closureReason : null,
            closureJustification: status === 'CLOSED' ? closureJustification : null,
          }
        : {}),
    }

    try {
      if (isEdit) {
        await updateRisk.mutateAsync(body)
      } else {
        const created = await createRisk.mutateAsync(body)
        navigate(`/risks/${created.id}`)
        return
      }
      navigate(`/risks/${id}`)
    } catch (err) {
      const e = err as ApiError
      setApiError(e.detail ?? 'Something went wrong.')
    }
  }

  const isPending = createRisk.isPending || updateRisk.isPending

  return (
    <div className="container pb-5" style={{ maxWidth: 680 }}>
      <div className="d-flex flex-column align-items-start gap-2 mb-4">
        <button className="btn btn-link p-0 text-decoration-none" onClick={() => navigate(-1)}>← Back</button>
        <h1 className="h3 mb-0">{isEdit ? 'Edit Risk' : 'New Risk'}</h1>
      </div>

      <form className="card card-body shadow-sm" onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label">Title</label>
          <input className="form-control" value={title} onChange={e => setTitle(e.target.value)} required />
        </div>

        <div className="mb-3">
          <label className="form-label">Description</label>
          <textarea className="form-control" value={description} onChange={e => setDescription(e.target.value)} />
        </div>

        <div className="row mb-3">
          <div className="col">
            <label className="form-label">Category</label>
            <SelectDropdown
              className="w-100"
              value={category}
              options={categories.map(c => ({ value: c, label: c.charAt(0) + c.slice(1).toLowerCase() }))}
              onChange={setCategory}
            />
          </div>
          <div className="col">
            <label className="form-label">Owner</label>
            <input className="form-control" value={owner} onChange={e => setOwner(e.target.value)} required />
          </div>
        </div>

        <div className="row mb-3 align-items-center">
          <div className="col-6">
            <div className="mb-3 d-flex align-items-center gap-2">
              <label className="form-label mb-0 score-field-label">Likelihood (1–5)</label>
              <SelectDropdown className="w-100" value={likelihood} options={scoreOptions} onChange={setLikelihood} />
            </div>
            <div className="d-flex align-items-center gap-2">
              <label className="form-label mb-0 score-field-label">Impact (1–5)</label>
              <SelectDropdown className="w-100" value={impact} options={scoreOptions} onChange={setImpact} />
            </div>
          </div>
          <div className="col-6">
            <div className="inherent-score-tile">
              <div className="text-uppercase small text-muted mb-1">Inherent</div>
              <div className="fs-2 fw-bold mb-1">{inherent}</div>
              <SeverityBadge band={band} />
            </div>
          </div>
        </div>

        {isEdit && (
          <div className="mb-3">
            <label className="form-label">Status</label>
            <SelectDropdown
              className="w-100"
              value={status}
              options={statuses.map(s => ({ value: s, label: s.charAt(0) + s.slice(1).toLowerCase() }))}
              onChange={setStatus}
            />
          </div>
        )}

        {isEdit && status === 'CLOSED' && (
          <div className="border rounded p-3 mb-3 bg-light">
            <div className="mb-3">
              <label className="form-label">Closure reason</label>
              <select className="form-select" value={closureReason}
                onChange={e => setClosureReason(e.target.value as ClosureReason)}>
                <option value="">— select —</option>
                {closureReasons.map(r => <option key={r} value={r}>{r.replace(/_/g, ' ').toLowerCase()}</option>)}
              </select>
            </div>
            <div>
              <label className="form-label">Justification</label>
              <textarea className="form-control" value={closureJustification}
                onChange={e => setClosureJustification(e.target.value)}
                placeholder="Required when closing without mitigations" />
            </div>
          </div>
        )}

        {apiError && <div className="alert alert-danger">{apiError}</div>}

        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-outline-secondary" onClick={() => navigate(-1)}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={isPending}>
            {isPending ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  )
}
