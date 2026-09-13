import type { SeverityBand } from '../api/types'

export const severityColor: Record<SeverityBand, string> = {
  LOW: '#2e7d32',
  MEDIUM: '#e9c46a',
  HIGH: '#f4a261',
  CRITICAL: '#e63946',
}

export const severityLabel: Record<SeverityBand, string> = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

export const severityRange: Record<SeverityBand, string> = {
  LOW: '1–5',
  MEDIUM: '6–12',
  HIGH: '13–19',
  CRITICAL: '20–25',
}

export const severityBands: SeverityBand[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
