package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VdotCalculatorTest {

    private final VdotCalculator calculator = new VdotCalculator();

    // --- calculateVdot ---

    @Test
    void calculateVdot_5kIn20Minutes_returnsExpectedVdot() {
        // 5K in 20:00 → ~49.8 VDOT (Daniels Running Formula)
        double vdot = calculator.calculateVdot(5_000, 20 * 60);
        assertThat(vdot).isCloseTo(49.8, within(0.5));
    }

    @Test
    void calculateVdot_marathonIn3Hours_returnsExpectedVdot() {
        // Marathon in 3:00:00 → ~53.5 VDOT (Daniels Running Formula)
        double vdot = calculator.calculateVdot(42_195, 3 * 3600);
        assertThat(vdot).isCloseTo(53.5, within(0.5));
    }

    @Test
    void calculateVdot_10kIn40Minutes_returnsExpectedVdot() {
        // 10K in 40:00 → ~51.9 VDOT (Daniels Running Formula)
        double vdot = calculator.calculateVdot(10_000, 40 * 60);
        assertThat(vdot).isCloseTo(51.9, within(0.5));
    }

    @Test
    void calculateVdot_halfMarathonIn90Minutes_returnsExpectedVdot() {
        // Half marathon in 1:30:00 → ~51.0 VDOT (Daniels Running Formula)
        double vdot = calculator.calculateVdot(21_097, 90 * 60);
        assertThat(vdot).isCloseTo(51.0, within(0.5));
    }

    @Test
    void calculateVdot_negativeDistance_throwsException() {
        assertThatThrownBy(() -> calculator.calculateVdot(-100, 600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateVdot_zeroTime_throwsException() {
        assertThatThrownBy(() -> calculator.calculateVdot(5_000, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- predictRaceTime ---

    @Test
    void predictRaceTime_vdot50For5k_returnsExpectedTime() {
        // VDOT 50 → 5K in ~19:36 (Daniels Running Formula, ~1196 seconds)
        double time = calculator.predictRaceTime(50.0, 5_000);
        assertThat(time).isCloseTo(1196, within(15.0));
    }

    @Test
    void predictRaceTime_vdot50ForMarathon_returnsExpectedTime() {
        // VDOT 50 → marathon in ~3:10:39 (Daniels Running Formula, ~11439 seconds)
        double time = calculator.predictRaceTime(50.0, 42_195);
        assertThat(time).isCloseTo(11439, within(60.0));
    }

    @Test
    void predictRaceTime_roundTrip_returnsOriginalTime() {
        // Calculate VDOT from a known race, then predict time for same distance
        double originalTime = 20 * 60; // 20:00 5K
        double vdot = calculator.calculateVdot(5_000, originalTime);
        double predictedTime = calculator.predictRaceTime(vdot, 5_000);
        assertThat(predictedTime).isCloseTo(originalTime, within(1.0));
    }

    @Test
    void predictRaceTime_vdotBelowRange_throwsException() {
        assertThatThrownBy(() -> calculator.predictRaceTime(20.0, 5_000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void predictRaceTime_vdotAboveRange_throwsException() {
        assertThatThrownBy(() -> calculator.predictRaceTime(90.0, 5_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
