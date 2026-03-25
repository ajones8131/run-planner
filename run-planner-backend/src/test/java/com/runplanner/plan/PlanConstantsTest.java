package com.runplanner.plan;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlanConstantsTest {

    @Test
    void phasePercentages_sumToOne() {
        double sum = PlanConstants.BASE_PHASE_PCT + PlanConstants.QUALITY_PHASE_PCT
                + PlanConstants.PEAK_PHASE_PCT + PlanConstants.TAPER_PHASE_PCT;
        assertThat(sum).isCloseTo(1.0, within(0.001));
    }

    @Test
    void weeklyTemplates_eachHaveSevenDays() {
        for (Map.Entry<TrainingPhase, WorkoutType[]> entry : PlanConstants.WEEKLY_TEMPLATES.entrySet()) {
            assertThat(entry.getValue()).hasSize(7)
                    .as("Template for %s should have 7 days", entry.getKey());
        }
    }

    @Test
    void weeklyTemplates_coverAllPhases() {
        assertThat(PlanConstants.WEEKLY_TEMPLATES).containsKeys(TrainingPhase.values());
    }

    @Test
    void distanceDistribution_longRunProportion_isValid() {
        assertThat(PlanConstants.LONG_RUN_PCT).isBetween(0.25, 0.30);
    }

    @Test
    void distanceDistribution_qualityProportion_isValid() {
        assertThat(PlanConstants.QUALITY_SESSION_PCT).isBetween(0.15, 0.20);
    }
}
