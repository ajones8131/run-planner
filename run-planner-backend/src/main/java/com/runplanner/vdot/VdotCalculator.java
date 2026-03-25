package com.runplanner.vdot;

import static com.runplanner.vdot.VdotConstants.*;

public class VdotCalculator {

    /**
     * Calculates VDOT score from a race performance.
     *
     * @param distanceMeters race distance in meters
     * @param timeSeconds    race finish time in seconds
     * @return VDOT score
     */
    public double calculateVdot(double distanceMeters, double timeSeconds) {
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (timeSeconds <= 0) {
            throw new IllegalArgumentException("Time must be positive");
        }

        double timeMinutes = timeSeconds / 60.0;
        double velocity = distanceMeters / timeMinutes; // meters per minute

        double oxygenCost = O2_INTERCEPT
                + O2_LINEAR * velocity
                + O2_QUADRATIC * velocity * velocity;

        double pctVO2max = PCT_BASE
                + PCT_COEFF_1 * Math.exp(PCT_EXP_1 * timeMinutes)
                + PCT_COEFF_2 * Math.exp(PCT_EXP_2 * timeMinutes);

        return oxygenCost / pctVO2max;
    }
}
