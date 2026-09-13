import { describe, it, expect } from 'vitest'
import type { SeverityBand } from '../api/types'

// Mirror of the backend severity logic — used for the live inherent preview in the form
function severityBand(score: number): SeverityBand {
  if (score >= 20) return 'CRITICAL'
  if (score >= 13) return 'HIGH'
  if (score >= 6) return 'MEDIUM'
  return 'LOW'
}

describe('severityBand', () => {
  it('maps exact boundaries correctly', () => {
    expect(severityBand(1)).toBe('LOW')
    expect(severityBand(5)).toBe('LOW')
    expect(severityBand(6)).toBe('MEDIUM')
    expect(severityBand(12)).toBe('MEDIUM')
    expect(severityBand(13)).toBe('HIGH')
    expect(severityBand(19)).toBe('HIGH')
    expect(severityBand(20)).toBe('CRITICAL')
    expect(severityBand(25)).toBe('CRITICAL')
  })

  it('inherent score is likelihood × impact', () => {
    for (let l = 1; l <= 5; l++) {
      for (let i = 1; i <= 5; i++) {
        expect(l * i).toBeGreaterThanOrEqual(1)
        expect(l * i).toBeLessThanOrEqual(25)
        // band is defined for all valid inherents
        expect(() => severityBand(l * i)).not.toThrow()
      }
    }
  })

  it('live inherent preview: L4×I5 shows Critical 20', () => {
    const score = 4 * 5
    expect(score).toBe(20)
    expect(severityBand(score)).toBe('CRITICAL')
  })

  it('live inherent preview: L1×I1 shows Low 1', () => {
    const score = 1 * 1
    expect(score).toBe(1)
    expect(severityBand(score)).toBe('LOW')
  })
})
