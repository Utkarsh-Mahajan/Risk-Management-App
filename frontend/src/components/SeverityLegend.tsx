import { severityBands, severityColor, severityLabel, severityRange } from './severity'

export function SeverityLegend() {
  return (
    <div className="d-flex flex-wrap gap-3 text-muted mb-3 severity-legend">
      <span className="fw-medium text-body">Severity:</span>
      {severityBands.map(band => (
        <span key={band} className="d-flex align-items-center gap-1">
          <span
            className="d-inline-block rounded-circle"
            style={{ width: 10, height: 10, backgroundColor: severityColor[band] }}
          />
          {severityLabel[band]} ({severityRange[band]})
        </span>
      ))}
    </div>
  )
}
