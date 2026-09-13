import type { SeverityBand } from '../api/types'
import { severityColor } from './severity'

interface Props {
  score: number
  band: SeverityBand
}

export function ScoreChip({ score, band }: Props) {
  return (
    <span
      className="d-inline-flex align-items-center justify-content-center rounded-circle text-white fw-semibold"
      style={{
        backgroundColor: severityColor[band],
        width: 32,
        height: 32,
        fontSize: '0.85rem',
        boxShadow: `0 0 0 3px ${severityColor[band]}1f`,
      }}
    >
      {score}
    </span>
  )
}
