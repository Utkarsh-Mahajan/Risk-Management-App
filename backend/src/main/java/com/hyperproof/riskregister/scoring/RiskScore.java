package com.hyperproof.riskregister.scoring;

import com.hyperproof.riskregister.domain.SeverityBand;

public record RiskScore(
    int inherent,
    SeverityBand inherentBand,
    int residual,
    SeverityBand residualBand
) {}
