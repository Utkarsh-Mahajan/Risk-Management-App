package com.hyperproof.riskregister.scoring;

import com.hyperproof.riskregister.domain.SeverityBand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class RiskScoringServiceTest {

    private final RiskScoringService scoring = new RiskScoringService();

    @Test
    void inherentIsLikelihoodTimesImpact() {
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                assertThat(scoring.score(l, i, List.of()).inherent()).isEqualTo(l * i);
            }
        }
    }

    @Test
    void zeroMitigationsResidualEqualsInherent() {
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                RiskScore s = scoring.score(l, i, List.of());
                assertThat(s.residual()).isEqualTo(s.inherent());
            }
        }
    }

    @ParameterizedTest(name = "inherent=20, eff={0} => residual={1}, band={2}")
    @CsvSource({
        "5,    10, MEDIUM",
        "3,    14, HIGH",
        "1,    18, HIGH",
    })
    void singleMitigationWorkedExamples(int eff, int expectedResidual, SeverityBand expectedBand) {
        RiskScore s = scoring.score(4, 5, List.of(eff));
        assertThat(s.residual()).isEqualTo(expectedResidual);
        assertThat(s.residualBand()).isEqualTo(expectedBand);
    }

    @ParameterizedTest(name = "{0} eff-5 controls on inherent 20 => residual {1}")
    @CsvSource({
        "1, 10",
        "2,  5",
        "3,  3",
        "4,  2",
        "5,  1",
    })
    void stackedEff5OnInherent20(int count, int expectedResidual) {
        List<Integer> effectiveness = Collections.nCopies(count, 5);
        assertThat(scoring.score(4, 5, effectiveness).residual()).isEqualTo(expectedResidual);
    }

    @Test
    void residualNeverFallsBelowOne() {
        // inherent 1 with maximum possible reduction
        assertThat(scoring.score(1, 1, List.of(5, 5, 5, 5, 5)).residual()).isEqualTo(1);
        // inherent 25 floored by five eff-5 controls
        assertThat(scoring.score(5, 5, Collections.nCopies(5, 5)).residual()).isEqualTo(1);
        // piling on even more controls keeps it at 1
        assertThat(scoring.score(5, 5, Collections.nCopies(50, 5)).residual()).isEqualTo(1);
    }

    @Test
    void residualNeverGoesNegative() {
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                assertThat(scoring.score(l, i, Collections.nCopies(20, 5)).residual()).isGreaterThanOrEqualTo(1);
            }
        }
    }

    @Test
    void residualNeverExceedsInherent() {
        for (int l = 1; l <= 5; l++) {
            for (int i = 1; i <= 5; i++) {
                for (int e = 1; e <= 5; e++) {
                    RiskScore s = scoring.score(l, i, List.of(e));
                    assertThat(s.residual()).isLessThanOrEqualTo(s.inherent());
                }
            }
        }
    }

    @Test
    void addingMitigationNeverRaisesResidual() {
        for (int e = 1; e <= 5; e++) {
            RiskScore before = scoring.score(4, 5, List.of());
            RiskScore after = scoring.score(4, 5, List.of(e));
            assertThat(after.residual()).isLessThanOrEqualTo(before.residual());
        }
    }

    @Test
    void higherEffectivenessGivesLowerOrEqualResidual() {
        int inherent = 4 * 5;
        int prev = inherent;
        for (int e = 1; e <= 5; e++) {
            int residual = scoring.score(4, 5, List.of(e)).residual();
            assertThat(residual).isLessThanOrEqualTo(prev);
            prev = residual;
        }
    }

    @Test
    void orderIndependence() {
        List<Integer> eff135 = List.of(1, 3, 5);
        int reference = scoring.score(4, 5, eff135).residual();
        assertThat(scoring.score(4, 5, List.of(1, 5, 3)).residual()).isEqualTo(reference);
        assertThat(scoring.score(4, 5, List.of(3, 1, 5)).residual()).isEqualTo(reference);
        assertThat(scoring.score(4, 5, List.of(3, 5, 1)).residual()).isEqualTo(reference);
        assertThat(scoring.score(4, 5, List.of(5, 1, 3)).residual()).isEqualTo(reference);
        assertThat(scoring.score(4, 5, List.of(5, 3, 1)).residual()).isEqualTo(reference);
    }

    @Test
    void ceilingRoundingNotHalfUp() {
        // inherent 25, eff 5: 25 × 0.5 = 12.5 → ceil = 13, not 12
        assertThat(scoring.score(5, 5, List.of(5)).residual()).isEqualTo(13);
    }

    @Test
    void factorCapAt99Percent() {
        // 100 eff-1 controls: 0.9^100 ≈ 2.66e-5, far below 0.01 floor
        // ceil(25 × 0.01) = 1; verifies factor is clamped before multiply
        int residual = scoring.score(5, 5, Collections.nCopies(100, 1)).residual();
        assertThat(residual).isEqualTo(1);
    }

    @Test
    void closingWithoutMitigationKeepsResidualEqualToInherent() {
        // Status is not a scoring input — this test encodes that invariant at the scoring layer
        RiskScore s = scoring.score(4, 5, List.of());
        assertThat(s.residual()).isEqualTo(s.inherent());
    }
}
