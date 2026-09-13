interface Props {
  inherent: number
  residual: number
}

export function Reduction({ inherent, residual }: Props) {
  if (inherent === residual) return <span className="chip chip-reduction-flat">—</span>
  const pct = Math.round(((inherent - residual) / inherent) * 100)
  return <span className="chip chip-reduction">−{pct}%</span>
}
