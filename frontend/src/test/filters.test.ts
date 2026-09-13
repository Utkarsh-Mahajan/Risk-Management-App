import { describe, it, expect } from 'vitest'
import type { Risk, RiskCategory, RiskStatus } from '../api/types'

// The API handles filtering and sorting; this tests the client-side helpers
// that drive the filter bar state and validate expected API behaviour.

function filterRisks(
  risks: Pick<Risk, 'category' | 'status' | 'residualScore'>[],
  category?: RiskCategory,
  status?: RiskStatus,
) {
  return risks.filter(r => {
    if (category && r.category !== category) return false
    if (status && r.status !== status) return false
    return true
  })
}

function sortByResidualDesc(risks: Pick<Risk, 'residualScore'>[]) {
  return [...risks].sort((a, b) => b.residualScore - a.residualScore)
}

const sample = [
  { category: 'SECURITY' as RiskCategory, status: 'OPEN' as RiskStatus, residualScore: 20 },
  { category: 'OPERATIONAL' as RiskCategory, status: 'CLOSED' as RiskStatus, residualScore: 5 },
  { category: 'SECURITY' as RiskCategory, status: 'CLOSED' as RiskStatus, residualScore: 10 },
  { category: 'FINANCIAL' as RiskCategory, status: 'OPEN' as RiskStatus, residualScore: 15 },
]

describe('filterRisks', () => {
  it('returns all when no filter is applied', () => {
    expect(filterRisks(sample)).toHaveLength(4)
  })

  it('filters by category', () => {
    const result = filterRisks(sample, 'SECURITY')
    expect(result).toHaveLength(2)
    expect(result.every(r => r.category === 'SECURITY')).toBe(true)
  })

  it('filters by status', () => {
    const result = filterRisks(sample, undefined, 'CLOSED')
    expect(result).toHaveLength(2)
    expect(result.every(r => r.status === 'CLOSED')).toBe(true)
  })

  it('filters by category and status together', () => {
    const result = filterRisks(sample, 'SECURITY', 'CLOSED')
    expect(result).toHaveLength(1)
    expect(result[0].residualScore).toBe(10)
  })
})

describe('sortByResidualDesc', () => {
  it('sorts highest residual first', () => {
    const sorted = sortByResidualDesc(sample)
    expect(sorted[0].residualScore).toBe(20)
    expect(sorted[sorted.length - 1].residualScore).toBe(5)
  })

  it('does not mutate the original array', () => {
    const original = [...sample]
    sortByResidualDesc(sample)
    expect(sample).toEqual(original)
  })
})
