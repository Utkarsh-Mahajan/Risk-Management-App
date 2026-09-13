package com.hyperproof.riskregister.service;

import com.hyperproof.riskregister.domain.*;
import com.hyperproof.riskregister.web.exception.BusinessRuleViolationException;
import com.hyperproof.riskregister.web.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RiskStatusTransitionRuleTest {

    private Risk risk;

    @BeforeEach
    void setUp() {
        risk = new Risk("Test", "desc", RiskCategory.SECURITY, "owner", 4, 5);
    }

    // Rule 1: closing with 0 mitigations, no reason → 422
    @Test
    void closeNoMitigationsNoReason() {
        assertThatExceptionOfType(BusinessRuleViolationException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.applyClose(risk, null, null))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("closure_reason_required"));
    }

    // Rule 2: closing with 0 mitigations, reason given, no justification → 422
    @Test
    void closeNoMitigationsNoJustification() {
        assertThatExceptionOfType(BusinessRuleViolationException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.applyClose(risk, ClosureReason.RISK_ACCEPTED, null))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("closure_justification_required"));
    }

    @Test
    void closeNoMitigationsBlankJustification() {
        assertThatExceptionOfType(BusinessRuleViolationException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.applyClose(risk, ClosureReason.RISK_ACCEPTED, "  "))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("closure_justification_required"));
    }

    // Rule 3: closing with 0 mitigations, reason MITIGATED → 422
    @Test
    void closeNoMitigationsWithMitigatedReason() {
        assertThatExceptionOfType(BusinessRuleViolationException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.applyClose(risk, ClosureReason.MITIGATED, "justified"))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("closure_reason_invalid"));
    }

    // Rule 4: closing with 0 mitigations + valid reason + justification → OK
    @Test
    void closeNoMitigationsValidReasonAndJustification() {
        RiskStatusTransitionRule.applyClose(risk, ClosureReason.RISK_ACCEPTED, "Accepted by CISO");
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.CLOSED);
        assertThat(risk.getClosureReason()).isEqualTo(ClosureReason.RISK_ACCEPTED);
        assertThat(risk.getClosedAt()).isNotNull();
    }

    // Rule 4: closing with ≥1 mitigation and no reason defaults to MITIGATED
    @Test
    void closeWithMitigationsDefaultsToMitigated() {
        risk.getMitigations().add(new Mitigation(risk, "control", 3));
        RiskStatusTransitionRule.applyClose(risk, null, null);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.CLOSED);
        assertThat(risk.getClosureReason()).isEqualTo(ClosureReason.MITIGATED);
    }

    // Rule 5: reopen clears closure fields and lands on the requested status
    @Test
    void reopenClearsClosure() {
        RiskStatusTransitionRule.applyClose(risk, ClosureReason.RISK_ACCEPTED, "no controls needed");
        RiskStatusTransitionRule.applyReopen(risk, RiskStatus.OPEN);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.OPEN);
        assertThat(risk.getClosureReason()).isNull();
        assertThat(risk.getClosureJustification()).isNull();
        assertThat(risk.getClosedAt()).isNull();
    }

    // Rule 5b: reopening a Mitigated-closed risk onto Mitigating keeps the requested status, not a hardcoded Open
    @Test
    void reopenOntoMitigatingKeepsRequestedStatus() {
        risk.getMitigations().add(new Mitigation(risk, "control", 3));
        RiskStatusTransitionRule.applyClose(risk, null, null);
        RiskStatusTransitionRule.applyReopen(risk, RiskStatus.MITIGATING);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.MITIGATING);
        assertThat(risk.getClosureReason()).isNull();
    }

    // Rule 6: deleting last mitigation of a MITIGATED closed risk → 409
    @Test
    void deletingLastMitigationOfClosedMitigatedRiskThrows() {
        Mitigation m = new Mitigation(risk, "control", 3);
        risk.getMitigations().add(m);
        RiskStatusTransitionRule.applyClose(risk, null, null);

        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.guardLastMitigationDelete(risk))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("closure_basis_removed"));
    }

    // Rule 7: adding a mitigation to a closed risk is blocked — reopen first
    @Test
    void addingMitigationToClosedRiskIsBlocked() {
        risk.getMitigations().add(new Mitigation(risk, "initial", 3));
        RiskStatusTransitionRule.applyClose(risk, null, null);

        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.guardMitigationAddOnClosedRisk(risk))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("risk_closed"));
    }

    // Rule 7 guard: passes silently for Open and Mitigating risks
    @Test
    void guardMitigationAddAllowedWhenNotClosed() {
        assertThatNoException().isThrownBy(() -> RiskStatusTransitionRule.guardMitigationAddOnClosedRisk(risk));
        risk.setStatus(RiskStatus.MITIGATING);
        assertThatNoException().isThrownBy(() -> RiskStatusTransitionRule.guardMitigationAddOnClosedRisk(risk));
    }

    // Rule 6 guard: passes silently when risk has >1 mitigation
    @Test
    void deletingNonLastMitigationOfClosedMitigatedRiskIsOk() {
        risk.getMitigations().add(new Mitigation(risk, "a", 3));
        risk.getMitigations().add(new Mitigation(risk, "b", 4));
        RiskStatusTransitionRule.applyClose(risk, null, null);
        assertThatNoException().isThrownBy(() -> RiskStatusTransitionRule.guardLastMitigationDelete(risk));
    }

    // Rule 8: adding a mitigation to an Open risk promotes it to Mitigating
    @Test
    void addingMitigationToOpenRiskPromotesToMitigating() {
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.OPEN);
        RiskStatusTransitionRule.applyMitigationAdded(risk);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.MITIGATING);
    }

    // Rule 8 idempotence: a risk already Mitigating stays Mitigating
    @Test
    void addingMitigationToMitigatingRiskStaysMitigating() {
        risk.setStatus(RiskStatus.MITIGATING);
        RiskStatusTransitionRule.applyMitigationAdded(risk);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.MITIGATING);
    }

    // Rule 8 exception: adding a mitigation never reopens a Closed risk
    @Test
    void addingMitigationToClosedRiskDoesNotReopenIt() {
        risk.getMitigations().add(new Mitigation(risk, "control", 3));
        RiskStatusTransitionRule.applyClose(risk, null, null);
        RiskStatusTransitionRule.applyMitigationAdded(risk);
        assertThat(risk.getStatus()).isEqualTo(RiskStatus.CLOSED);
    }

    // Rule 9: cannot set status to Open while mitigations are attached
    @Test
    void guardOpenRejectsWhenMitigationsExist() {
        risk.getMitigations().add(new Mitigation(risk, "control", 3));
        assertThatExceptionOfType(ConflictException.class)
                .isThrownBy(() -> RiskStatusTransitionRule.guardOpenRequiresNoMitigations(risk))
                .satisfies(e -> assertThat(e.getCode()).isEqualTo("mitigations_exist"));
    }

    // Rule 9 guard: passes silently when there are no mitigations
    @Test
    void guardOpenAllowsWhenNoMitigations() {
        assertThatNoException().isThrownBy(() -> RiskStatusTransitionRule.guardOpenRequiresNoMitigations(risk));
    }
}
