package com.hyperproof.riskregister.domain;

public enum SeverityBand {
    LOW(1, 5),
    MEDIUM(6, 12),
    HIGH(13, 19),
    CRITICAL(20, 25);

    private final int min;
    private final int max;

    SeverityBand(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static SeverityBand of(int score) {
        for (SeverityBand band : values()) {
            if (score >= band.min && score <= band.max) {
                return band;
            }
        }
        throw new IllegalArgumentException("Score %d is outside the valid range 1–25".formatted(score));
    }
}
