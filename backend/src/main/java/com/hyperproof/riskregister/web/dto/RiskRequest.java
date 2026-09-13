package com.hyperproof.riskregister.web.dto;

import com.hyperproof.riskregister.domain.ClosureReason;
import com.hyperproof.riskregister.domain.RiskCategory;
import com.hyperproof.riskregister.domain.RiskStatus;
import jakarta.validation.constraints.*;

public record RiskRequest(
    @NotBlank String title,
    String description,
    @NotNull RiskCategory category,
    @NotBlank String owner,
    @NotNull @Min(1) @Max(5) Integer likelihood,
    @NotNull @Min(1) @Max(5) Integer impact,
    RiskStatus status,
    ClosureReason closureReason,
    String closureJustification
) {}
