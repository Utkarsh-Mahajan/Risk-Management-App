export type RiskCategory = 'OPERATIONAL' | 'FINANCIAL' | 'COMPLIANCE' | 'SECURITY' | 'STRATEGIC'
export type RiskStatus = 'OPEN' | 'MITIGATING' | 'CLOSED'
export type ClosureReason = 'MITIGATED' | 'RISK_ACCEPTED' | 'TRANSFERRED' | 'NO_LONGER_APPLICABLE' | 'DUPLICATE'
export type SeverityBand = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export interface Mitigation {
  id: number
  description: string
  effectiveness: number
  createdAt: string
}

export interface Risk {
  id: number
  title: string
  description: string | null
  category: RiskCategory
  owner: string
  likelihood: number
  impact: number
  status: RiskStatus
  closureReason: ClosureReason | null
  closureJustification: string | null
  closedAt: string | null
  inherentScore: number
  inherentBand: SeverityBand
  residualScore: number
  residualBand: SeverityBand
  mitigationCount: number
  mitigations: Mitigation[]
  createdAt: string
  updatedAt: string
}

export interface RiskRequest {
  title: string
  description: string
  category: RiskCategory
  owner: string
  likelihood: number
  impact: number
  status?: RiskStatus
  closureReason?: ClosureReason | null
  closureJustification?: string | null
}

export interface MitigationRequest {
  description: string
  effectiveness: number
}

export interface ApiError {
  status: number
  title: string
  detail: string
  code: string
  fields?: Record<string, string>
}
