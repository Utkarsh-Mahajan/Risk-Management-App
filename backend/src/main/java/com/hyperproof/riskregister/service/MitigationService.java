package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.Mitigation;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.repository.MitigationRepository;
import com.hyperproof.riskregister.repository.RiskRepository;
import com.hyperproof.riskregister.scoring.RiskScoringService;
import com.hyperproof.riskregister.web.dto.MitigationRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MitigationService {

    private final RiskRepository risks;
    private final MitigationRepository mitigations;
    private final RiskScoringService scoring;

    public MitigationService(RiskRepository risks, MitigationRepository mitigations, RiskScoringService scoring) {
        this.risks = risks;
        this.mitigations = mitigations;
        this.scoring = scoring;
    }

    public RiskResponse create(Long riskId, MitigationRequest req) {
        Risk risk = findRiskOrThrow(riskId);
        RiskStatusTransitionRule.guardMitigationAddOnClosedRisk(risk);
        risk.getMitigations().add(new Mitigation(risk, req.description(), req.effectiveness()));
        RiskStatusTransitionRule.applyMitigationAdded(risk);
        return RiskResponse.from(risk, scoring.score(risk));
    }

    public RiskResponse update(Long riskId, Long mitigationId, MitigationRequest req) {
        Risk risk = findRiskOrThrow(riskId);
        Mitigation mitigation = findMitigationOrThrow(mitigationId, riskId);
        mitigation.setDescription(req.description());
        mitigation.setEffectiveness(req.effectiveness());
        return RiskResponse.from(risk, scoring.score(risk));
    }

    public void delete(Long riskId, Long mitigationId) {
        Risk risk = findRiskOrThrow(riskId);
        Mitigation mitigation = findMitigationOrThrow(mitigationId, riskId);
        RiskStatusTransitionRule.guardLastMitigationDelete(risk);
        risk.getMitigations().remove(mitigation);
    }

    private Risk findRiskOrThrow(Long riskId) {
        return risks.findWithMitigationsById(riskId)
                .orElseThrow(() -> new EntityNotFoundException("Risk not found: " + riskId));
    }

    private Mitigation findMitigationOrThrow(Long mitigationId, Long riskId) {
        Mitigation m = mitigations.findById(mitigationId)
                .orElseThrow(() -> new EntityNotFoundException("Mitigation not found: " + mitigationId));
        if (!m.getRisk().getId().equals(riskId)) {
            throw new EntityNotFoundException("Mitigation %d does not belong to risk %d".formatted(mitigationId, riskId));
        }
        return m;
    }
}
