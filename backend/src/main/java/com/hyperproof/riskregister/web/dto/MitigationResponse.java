package com.hyperproof.riskregister.web.dto;

import com.hyperproof.riskregister.domain.Mitigation;

import java.time.Instant;

public record MitigationResponse(
    Long id,
    String description,
    int effectiveness,
    Instant createdAt
) {
    public static MitigationResponse from(Mitigation m) {
        return new MitigationResponse(m.getId(), m.getDescription(), m.getEffectiveness(), m.getCreatedAt());
    }
}
