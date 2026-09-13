import type { RiskStatus } from '../api/types'

export const statusChipClass: Record<RiskStatus, string> = {
  OPEN: 'chip chip-status-open',
  MITIGATING: 'chip chip-status-mitigating',
  CLOSED: 'chip chip-status-closed',
}
