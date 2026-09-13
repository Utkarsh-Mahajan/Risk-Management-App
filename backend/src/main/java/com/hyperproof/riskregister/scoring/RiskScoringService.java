package com.hyperproof.riskregister.scoring;

import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.SeverityBand;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Controls are layered defenses: each one reduces what the previous ones left behind.
 * factor = Π(1 − effectiveness/10), clamped to 0.01 so total reduction never exceeds 99%.
 * Rounding (CEILING) happens once at the end — rounding per-control would make the result
 * order-dependent, because ceil(20 × 0.5) × 0.7 ≠ ceil(20 × 0.7) × 0.5.
 */
@Service
public class RiskScoringService {

    private static final BigDecimal FACTOR_FLOOR = new BigDecimal("0.01");

    public RiskScore score(Risk risk) {
        List<Integer> effectiveness = risk.getMitigations().stream()
                .map(m -> m.getEffectiveness())
                .toList();
        return score(risk.getLikelihood(), risk.getImpact(), effectiveness);
    }

    public RiskScore score(int likelihood, int impact, List<Integer> effectiveness) {
        int inherent = likelihood * impact;

        BigDecimal factor = BigDecimal.ONE;
        for (int e : effectiveness) {
            BigDecimal reduction = new BigDecimal(e).movePointLeft(1); // e/10
            factor = factor.multiply(BigDecimal.ONE.subtract(reduction));
            if (factor.compareTo(FACTOR_FLOOR) <= 0) {
                factor = FACTOR_FLOOR;
                break;
            }
        }

        int residual = Math.max(1,
                new BigDecimal(inherent).multiply(factor)
                        .setScale(0, RoundingMode.CEILING)
                        .intValue());

        return new RiskScore(
                inherent, SeverityBand.of(inherent),
                residual, SeverityBand.of(residual));
    }
}
