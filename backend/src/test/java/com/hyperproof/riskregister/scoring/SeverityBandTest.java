package com.hyperproof.riskregister.scoring;

import com.hyperproof.riskregister.domain.SeverityBand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class SeverityBandTest {

    @ParameterizedTest
    @CsvSource({
        "1,  LOW",
        "5,  LOW",
        "6,  MEDIUM",
        "12, MEDIUM",
        "13, HIGH",
        "19, HIGH",
        "20, CRITICAL",
        "25, CRITICAL",
    })
    void exactBoundaries(int score, SeverityBand expected) {
        assertThat(SeverityBand.of(score)).isEqualTo(expected);
    }

    @Test
    void zeroThrows() {
        assertThatIllegalArgumentException().isThrownBy(() -> SeverityBand.of(0));
    }

    @Test
    void twentySixThrows() {
        assertThatIllegalArgumentException().isThrownBy(() -> SeverityBand.of(26));
    }
}
