package com.hyperproof.riskregister.web.dto;

import com.hyperproof.riskregister.domain.ClosureReason;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.RiskCategory;
import com.hyperproof.riskregister.domain.RiskStatus;
import com.hyperproof.riskregister.domain.SeverityBand;
import com.hyperproof.riskregister.scoring.RiskScore;

import java.time.Instant;
import java.util.List;

public record RiskResponse(
    Long id,
    String title,
    String description,
    RiskCategory category,
    String owner,
    int likelihood,
    int impact,
    RiskStatus status,
    ClosureReason closureReason,
    String closureJustification,
    Instant closedAt,
    int inherentScore,
    SeverityBand inherentBand,
    int residualScore,
    SeverityBand residualBand,
    int mitigationCount,
    List<MitigationResponse> mitigations,
    Instant createdAt,
    Instant updatedAt
) {
    public static RiskResponse from(Risk risk, RiskScore score) {
        return new RiskResponse(
                risk.getId(),
                risk.getTitle(),
                risk.getDescription(),
                risk.getCategory(),
                risk.getOwner(),
                risk.getLikelihood(),
                risk.getImpact(),
                risk.getStatus(),
                risk.getClosureReason(),
                risk.getClosureJustification(),
                risk.getClosedAt(),
                score.inherent(),
                score.inherentBand(),
                score.residual(),
                score.residualBand(),
                risk.getMitigations().size(),
                risk.getMitigations().stream().map(MitigationResponse::from).toList(),
                risk.getCreatedAt(),
                risk.getUpdatedAt()
        );
    }
}
