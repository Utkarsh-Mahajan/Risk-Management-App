package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.*;
import com.hyperproof.riskregister.repository.RiskRepository;
import com.hyperproof.riskregister.scoring.RiskScore;
import com.hyperproof.riskregister.scoring.RiskScoringService;
import com.hyperproof.riskregister.web.dto.RiskRequest;
import com.hyperproof.riskregister.web.dto.RiskResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class RiskService {

    private final RiskRepository risks;
    private final RiskScoringService scoring;

    public RiskService(RiskRepository risks, RiskScoringService scoring) {
        this.risks = risks;
        this.scoring = scoring;
    }

    @Transactional(readOnly = true)
    public List<RiskResponse> list(RiskCategory category, RiskStatus status) {
        List<Risk> found;
        if (category != null && status != null) {
            found = risks.findByCategoryAndStatusWithMitigations(category, status);
        } else if (category != null) {
            found = risks.findByCategoryWithMitigations(category);
        } else if (status != null) {
            found = risks.findByStatusWithMitigations(status);
        } else {
            found = risks.findAllWithMitigations();
        }
        return found.stream()
                .map(r -> RiskResponse.from(r, scoring.score(r)))
                .sorted(Comparator.comparingInt(RiskResponse::residualScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskResponse get(Long id) {
        Risk risk = findOrThrow(id);
        return RiskResponse.from(risk, scoring.score(risk));
    }

    public RiskResponse create(RiskRequest req) {
        Risk risk = new Risk(req.title(), req.description(), req.category(),
                req.owner(), req.likelihood(), req.impact());
        Risk saved = risks.save(risk);
        return RiskResponse.from(saved, scoring.score(saved));
    }

    public RiskResponse update(Long id, RiskRequest req) {
        Risk risk = findOrThrow(id);

        risk.setTitle(req.title());
        risk.setDescription(req.description());
        risk.setCategory(req.category());
        risk.setOwner(req.owner());
        risk.setLikelihood(req.likelihood());
        risk.setImpact(req.impact());

        RiskStatus newStatus = req.status() != null ? req.status() : risk.getStatus();
        if (newStatus == RiskStatus.OPEN) {
            RiskStatusTransitionRule.guardOpenRequiresNoMitigations(risk);
        }

        if (newStatus == RiskStatus.CLOSED && risk.getStatus() != RiskStatus.CLOSED) {
            RiskStatusTransitionRule.applyClose(risk, req.closureReason(), req.closureJustification());
        } else if (newStatus != RiskStatus.CLOSED && risk.getStatus() == RiskStatus.CLOSED) {
            RiskStatusTransitionRule.applyReopen(risk, newStatus);
        } else {
            risk.setStatus(newStatus);
        }

        return RiskResponse.from(risk, scoring.score(risk));
    }

    public void delete(Long id) {
        if (!risks.existsById(id)) {
            throw new EntityNotFoundException("Risk not found: " + id);
        }
        risks.deleteById(id);
    }

    private Risk findOrThrow(Long id) {
        return risks.findWithMitigationsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Risk not found: " + id));
    }
}
