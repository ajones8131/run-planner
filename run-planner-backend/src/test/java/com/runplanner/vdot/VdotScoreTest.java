package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VdotScoreTest {

    private static Map<TrainingZone, PaceRange> validPaces() {
        return new TrainingPaceCalculator().calculate(50.0);
    }

    @Test
    void construction_validScoreAndAllZones_succeeds() {
        Map<TrainingZone, PaceRange> paces = validPaces();

        VdotScore score = new VdotScore(50.0, paces);

        assertThat(score.score()).isEqualTo(50.0);
        assertThat(score.trainingPaces()).containsKeys(TrainingZone.values());
    }

    @Test
    void construction_scoreBelowMinVdot_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new VdotScore(VdotConstants.MIN_VDOT - 0.1, validPaces()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VDOT score must be between");
    }

    @Test
    void construction_scoreAboveMaxVdot_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new VdotScore(VdotConstants.MAX_VDOT + 0.1, validPaces()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VDOT score must be between");
    }

    @Test
    void construction_nullTrainingPaces_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new VdotScore(50.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Training paces must include all zones");
    }

    @Test
    void construction_incompleteZoneMap_throwsIllegalArgumentException() {
        Map<TrainingZone, PaceRange> incomplete = new EnumMap<>(TrainingZone.class);
        // Populate all zones except one
        Map<TrainingZone, PaceRange> full = validPaces();
        full.entrySet().stream()
                .filter(e -> e.getKey() != TrainingZone.R)
                .forEach(e -> incomplete.put(e.getKey(), e.getValue()));

        assertThatThrownBy(() -> new VdotScore(50.0, incomplete))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Training paces must include all zones");
    }
}
