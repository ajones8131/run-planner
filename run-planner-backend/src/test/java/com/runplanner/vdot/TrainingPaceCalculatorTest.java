package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TrainingPaceCalculatorTest {

    private final TrainingPaceCalculator calculator = new TrainingPaceCalculator();

    @Test
    void calculate_allFiveZonesPresent() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        assertThat(paces).containsKeys(TrainingZone.values());
    }

    @Test
    void calculate_minPaceFasterThanMaxPaceForAllZones() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        paces.forEach((zone, range) ->
                assertThat(range.minPaceMinPerKm())
                        .as("Zone %s: min (faster) should be <= max (slower)", zone)
                        .isLessThanOrEqualTo(range.maxPaceMinPerKm()));
    }

    @Test
    void calculate_easyIsSlowestAndRepIsFastest() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        // Easy max (slowest boundary) should be slower than Rep min (fastest boundary)
        assertThat(paces.get(TrainingZone.E).maxPaceMinPerKm())
                .isGreaterThan(paces.get(TrainingZone.R).minPaceMinPerKm());
    }

    @Test
    void calculate_higherVdotProducesFasterPaces() {
        Map<TrainingZone, PaceRange> lowVdot = calculator.calculate(40.0);
        Map<TrainingZone, PaceRange> highVdot = calculator.calculate(60.0);

        for (TrainingZone zone : TrainingZone.values()) {
            assertThat(highVdot.get(zone).minPaceMinPerKm())
                    .as("Zone %s: higher VDOT should produce faster min pace", zone)
                    .isLessThan(lowVdot.get(zone).minPaceMinPerKm());
            assertThat(highVdot.get(zone).maxPaceMinPerKm())
                    .as("Zone %s: higher VDOT should produce faster max pace", zone)
                    .isLessThan(lowVdot.get(zone).maxPaceMinPerKm());
        }
    }

    @Test
    void calculate_vdot50EasyPace_matchesFormula() {
        // VDOT 50 → Easy (59-74% VO2max) → pace ~4.89-5.86 min/km (formula-derived)
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        PaceRange easy = paces.get(TrainingZone.E);
        // Allow ±0.15 min/km (~9 sec/km) tolerance
        assertThat(easy.minPaceMinPerKm()).isCloseTo(4.89, within(0.15));
        assertThat(easy.maxPaceMinPerKm()).isCloseTo(5.86, within(0.15));
    }

    @Test
    void calculate_vdot50ThresholdPace_matchesFormula() {
        // VDOT 50 → Threshold (83-88% VO2max) → pace ~4.25-4.46 min/km (formula-derived)
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        PaceRange threshold = paces.get(TrainingZone.T);
        assertThat(threshold.minPaceMinPerKm()).isCloseTo(4.25, within(0.15));
        assertThat(threshold.maxPaceMinPerKm()).isCloseTo(4.46, within(0.15));
    }

    @Test
    void calculate_belowMinVdot_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(29.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_aboveMaxVdot_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(86.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
