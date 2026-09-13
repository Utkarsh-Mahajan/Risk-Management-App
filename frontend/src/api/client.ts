import type { ApiError, MitigationRequest, Risk, RiskCategory, RiskRequest, RiskStatus } from './types'

const BASE = '/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  })

  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw { status: res.status, ...err } as ApiError
  }

  if (res.status === 204) return undefined as T
  return res.json()
}

export const api = {
  risks: {
    list(params?: { category?: RiskCategory; status?: RiskStatus }): Promise<Risk[]> {
      const qs = new URLSearchParams()
      if (params?.category) qs.set('category', params.category)
      if (params?.status) qs.set('status', params.status)
      const query = qs.toString() ? `?${qs}` : ''
      return request(`/risks${query}`)
    },

    get(id: number): Promise<Risk> {
      return request(`/risks/${id}`)
    },

    create(body: RiskRequest): Promise<Risk> {
      return request('/risks', { method: 'POST', body: JSON.stringify(body) })
    },

    update(id: number, body: RiskRequest): Promise<Risk> {
      return request(`/risks/${id}`, { method: 'PATCH', body: JSON.stringify(body) })
    },

    delete(id: number): Promise<void> {
      return request(`/risks/${id}`, { method: 'DELETE' })
    },
  },

  mitigations: {
    create(riskId: number, body: MitigationRequest): Promise<Risk> {
      return request(`/risks/${riskId}/mitigations`, { method: 'POST', body: JSON.stringify(body) })
    },

    update(riskId: number, id: number, body: MitigationRequest): Promise<Risk> {
      return request(`/risks/${riskId}/mitigations/${id}`, { method: 'PATCH', body: JSON.stringify(body) })
    },

    delete(riskId: number, id: number): Promise<void> {
      return request(`/risks/${riskId}/mitigations/${id}`, { method: 'DELETE' })
    },
  },
}
