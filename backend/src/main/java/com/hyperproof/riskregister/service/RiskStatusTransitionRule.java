package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.ClosureReason;
import com.hyperproof.riskregister.domain.Risk;
import com.hyperproof.riskregister.domain.RiskStatus;
import com.hyperproof.riskregister.web.exception.BusinessRuleViolationException;
import com.hyperproof.riskregister.web.exception.ConflictException;

import java.time.Instant;

public final class RiskStatusTransitionRule {

    private RiskStatusTransitionRule() {}

    public static void applyClose(Risk risk, ClosureReason reason, String justification) {
        boolean hasMitigations = !risk.getMitigations().isEmpty();

        if (!hasMitigations) {
            // closing with no controls — must explain why
            if (reason == null) {
                throw new BusinessRuleViolationException("closure_reason_required",
                        "A closure reason is required when closing a risk with no mitigations.");
            }
            if (reason == ClosureReason.MITIGATED) {
                // cannot claim mitigated with no controls on record
                throw new BusinessRuleViolationException("closure_reason_invalid",
                        "Cannot close as MITIGATED with no mitigations. Choose RISK_ACCEPTED, TRANSFERRED, NO_LONGER_APPLICABLE, or DUPLICATE.");
            }
            if (justification == null || justification.isBlank()) {
                throw new BusinessRuleViolationException("closure_justification_required",
                        "A justification is required when closing a risk with no mitigations.");
            }
        }

        risk.setStatus(RiskStatus.CLOSED);
        risk.setClosureReason(hasMitigations && reason == null ? ClosureReason.MITIGATED : reason);
        risk.setClosureJustification(justification);
        risk.setClosedAt(Instant.now());
    }

    public static void applyReopen(Risk risk, RiskStatus newStatus) {
        risk.setStatus(newStatus);
        risk.setClosureReason(null);
        risk.setClosureJustification(null);
        risk.setClosedAt(null);
    }

    public static void applyMitigationAdded(Risk risk) {
        // a control landing on an Open risk means mitigation work has started
        if (risk.getStatus() == RiskStatus.OPEN) {
            risk.setStatus(RiskStatus.MITIGATING);
        }
    }

    public static void guardMitigationAddOnClosedRisk(Risk risk) {
        // a closed risk's score and closure reason are a settled record — reopen before adding controls
        if (risk.getStatus() == RiskStatus.CLOSED) {
            throw new ConflictException("risk_closed",
                    "Cannot add a mitigation to a closed risk. Reopen the risk first, then add the control.");
        }
    }

    public static void guardOpenRequiresNoMitigations(Risk risk) {
        // "Open" means nobody has started addressing this risk yet — that claim must hold
        if (!risk.getMitigations().isEmpty()) {
            throw new ConflictException("mitigations_exist",
                    "Cannot set status to Open while mitigations are attached. Choose Mitigating, or remove the mitigations first.");
        }
    }

    public static void guardLastMitigationDelete(Risk risk) {
        // deleting the sole basis for a MITIGATED closure invalidates the closure claim
        if (risk.getStatus() == RiskStatus.CLOSED
                && risk.getClosureReason() == ClosureReason.MITIGATED
                && risk.getMitigations().size() == 1) {
            throw new ConflictException("closure_basis_removed",
                    "Cannot delete the last mitigation of a risk closed as MITIGATED. Reopen the risk or change the closure reason first.");
        }
    }
}
