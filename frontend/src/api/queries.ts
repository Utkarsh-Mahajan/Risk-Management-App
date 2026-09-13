import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type { MitigationRequest, RiskCategory, RiskRequest, RiskStatus } from './types'

export const keys = {
  risks: (params?: { category?: RiskCategory; status?: RiskStatus }) =>
    ['risks', params ?? {}] as const,
  risk: (id: number) => ['risks', id] as const,
}

export function useRisks(params?: { category?: RiskCategory; status?: RiskStatus }) {
  return useQuery({ queryKey: keys.risks(params), queryFn: () => api.risks.list(params) })
}

export function useRisk(id: number) {
  return useQuery({ queryKey: keys.risk(id), queryFn: () => api.risks.get(id) })
}

export function useCreateRisk() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: RiskRequest) => api.risks.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}

export function useUpdateRisk(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: RiskRequest) => api.risks.update(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}

export function useDeleteRisk() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.risks.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}

export function useCreateMitigation(riskId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: MitigationRequest) => api.mitigations.create(riskId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}

export function useUpdateMitigation(riskId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: MitigationRequest }) =>
      api.mitigations.update(riskId, id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}

export function useDeleteMitigation(riskId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => api.mitigations.delete(riskId, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['risks'] }),
  })
}
