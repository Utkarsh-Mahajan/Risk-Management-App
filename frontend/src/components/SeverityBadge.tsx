import type { SeverityBand } from '../api/types'
import { severityColor, severityLabel } from './severity'

interface Props {
  band: SeverityBand
}

export function SeverityBadge({ band }: Props) {
  return (
    <span className="badge text-white" style={{ backgroundColor: severityColor[band] }}>
      {severityLabel[band]}
    </span>
  )
}
