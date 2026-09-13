package com.hyperproof.riskregister.web.dto;

import jakarta.validation.constraints.*;

public record MitigationRequest(
    @NotBlank String description,
    @NotNull @Min(1) @Max(5) Integer effectiveness
) {}
