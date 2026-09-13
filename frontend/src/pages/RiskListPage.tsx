import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { RiskCategory, RiskStatus } from '../api/types'
import { useRisks } from '../api/queries'
import { ScoreChip } from '../components/ScoreChip'
import { SeverityLegend } from '../components/SeverityLegend'
import { Reduction } from '../components/Reduction'
import { severityColor } from '../components/severity'
import { statusChipClass } from '../components/statusChip'
import { FilterDropdown } from '../components/FilterDropdown'

const categories: RiskCategory[] = ['OPERATIONAL', 'FINANCIAL', 'COMPLIANCE', 'SECURITY', 'STRATEGIC']
const statuses: RiskStatus[] = ['OPEN', 'MITIGATING', 'CLOSED']

export function RiskListPage() {
  const navigate = useNavigate()
  const [category, setCategory] = useState<RiskCategory | ''>('')
  const [status, setStatus] = useState<RiskStatus | ''>('')

  const { data: risks, isLoading, error } = useRisks({
    category: category || undefined,
    status: status || undefined,
  })

  return (
    <div className="container pb-5">
      <h1 className="h3 mb-4">All Risks</h1>

      <div className="row g-4 align-items-center mb-4">
        <div className="col-auto">
          <FilterDropdown
            label="Category: "
            value={category}
            placeholder="All categories"
            options={categories.map(c => ({ value: c, label: c.charAt(0) + c.slice(1).toLowerCase() }))}
            onChange={setCategory}
          />
        </div>

        <div className="col-auto">
          <FilterDropdown
            label="Status: "
            value={status}
            placeholder="All statuses"
            options={statuses.map(s => ({ value: s, label: s.charAt(0) + s.slice(1).toLowerCase() }))}
            onChange={setStatus}
          />
        </div>
      </div>

      <SeverityLegend />

      {isLoading && <p>Loading…</p>}
      {error && <div className="alert alert-danger">Failed to load risks.</div>}

      {risks && risks.length === 0 && (
        <div className="text-center text-muted py-5">No risks found.</div>
      )}

      {risks && risks.length > 0 && (
        <div className="mui-table-card">
          <div className="table-responsive mui-table-scroll">
            <table className="mui-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Inherent</th>
                  <th>Residual</th>
                  <th>Reduction</th>
                  <th>Controls</th>
                  <th>Category</th>
                  <th>Status</th>
                  <th>Owner</th>
                </tr>
              </thead>
              <tbody>
                {risks.map(risk => {
                  const isUnmitigatedAccepted =
                    risk.status === 'CLOSED' &&
                    risk.mitigationCount === 0 &&
                    risk.closureReason !== 'MITIGATED'

                  return (
                    <tr key={risk.id}>
                      <td style={{ borderLeft: `4px solid ${severityColor[risk.residualBand]}` }}>
                        <button
                          className="btn btn-link p-0 risk-title-link"
                          onClick={() => navigate(`/risks/${risk.id}`)}
                        >
                          {risk.title}
                        </button>
                      </td>
                      <td>
                        <ScoreChip score={risk.inherentScore} band={risk.inherentBand} />
                      </td>
                      <td>
                        <ScoreChip score={risk.residualScore} band={risk.residualBand} />
                      </td>
                      <td>
                        <Reduction inherent={risk.inherentScore} residual={risk.residualScore} />
                      </td>
                      <td>{risk.mitigationCount}</td>
                      <td>
                        <span className="chip chip-category">
                          {risk.category.charAt(0) + risk.category.slice(1).toLowerCase()}
                        </span>
                      </td>
                      <td>
                        <span className={statusChipClass[risk.status]}>
                          {risk.status.charAt(0) + risk.status.slice(1).toLowerCase()}
                          {isUnmitigatedAccepted && ' (Unmitigated)'}
                        </span>
                      </td>
                      <td>{risk.owner}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
