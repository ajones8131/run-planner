package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VdotCalculatorTest {

    private final VdotCalculator calculator = new VdotCalculator();

    // --- calculateVdot ---

    @Test
    void calculateVdot_5kIn20Minutes_returnsExpectedVdot() {
        // 5K in 20:00 → ~42.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(5_000, 20 * 60);
        assertThat(vdot).isCloseTo(42.2, within(0.5));
    }

    @Test
    void calculateVdot_marathonIn3Hours_returnsExpectedVdot() {
        // Marathon in 3:00:00 → ~54.6 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(42_195, 3 * 3600);
        assertThat(vdot).isCloseTo(54.6, within(0.5));
    }

    @Test
    void calculateVdot_10kIn40Minutes_returnsExpectedVdot() {
        // 10K in 40:00 → ~44.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(10_000, 40 * 60);
        assertThat(vdot).isCloseTo(44.2, within(0.5));
    }

    @Test
    void calculateVdot_halfMarathonIn90Minutes_returnsExpectedVdot() {
        // Half marathon in 1:30:00 → ~52.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(21_097, 90 * 60);
        assertThat(vdot).isCloseTo(52.2, within(0.5));
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
}
