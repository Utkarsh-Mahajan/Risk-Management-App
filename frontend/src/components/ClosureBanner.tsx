import type { Risk } from '../api/types'

const reasonLabels: Record<string, string> = {
  MITIGATED: 'Mitigated',
  RISK_ACCEPTED: 'Risk accepted',
  TRANSFERRED: 'Transferred',
  NO_LONGER_APPLICABLE: 'No longer applicable',
  DUPLICATE: 'Duplicate',
}

interface Props {
  risk: Risk
}

export function ClosureBanner({ risk }: Props) {
  if (risk.status !== 'CLOSED' || !risk.closureReason) return null

  const unmitigatedHighSeverity =
    risk.mitigationCount === 0 &&
    (risk.residualBand === 'HIGH' || risk.residualBand === 'CRITICAL')

  return (
    <div className={`alert ${unmitigatedHighSeverity ? 'alert-danger' : 'alert-secondary'} mb-4`}>
      <div className="fw-semibold">
        {unmitigatedHighSeverity ? '⚠ Closed without mitigations — ' : 'Closed — '}
        {reasonLabels[risk.closureReason]}
      </div>
      {risk.closureJustification && (
        <div className="text-muted mt-2 mb-0">{risk.closureJustification}</div>
      )}
    </div>
  )
}
