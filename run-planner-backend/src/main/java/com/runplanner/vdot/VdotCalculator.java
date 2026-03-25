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

    /**
     * Predicts race finish time for a given VDOT and distance using binary search.
     *
     * @param vdotScore      the VDOT score (must be between MIN_VDOT and MAX_VDOT)
     * @param distanceMeters race distance in meters
     * @return predicted finish time in seconds
     */
    public double predictRaceTime(double vdotScore, double distanceMeters) {
        if (vdotScore < MIN_VDOT || vdotScore > MAX_VDOT) {
            throw new IllegalArgumentException(
                    "VDOT must be between " + MIN_VDOT + " and " + MAX_VDOT);
        }
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }

        // Binary search: find time where calculateVdot(distance, time) == vdotScore.
        // With the Daniels formula, faster time → higher VDOT (monotone over race paces).
        // Bounds span a wide enough range to cover VDOT 30–85 for any race distance.
        double lowSeconds = distanceMeters / 1000.0 * 60;    // 1 min/km (fastest plausible)
        double highSeconds = distanceMeters / 1000.0 * 900;  // 15 min/km (slowest plausible)

        for (int i = 0; i < 100; i++) {
            double midSeconds = (lowSeconds + highSeconds) / 2.0;
            double computedVdot = calculateVdot(distanceMeters, midSeconds);

            if (Math.abs(computedVdot - vdotScore) < 0.001) {
                return midSeconds;
            }

            // Faster time → higher VDOT; if computed VDOT exceeds target, time is too fast.
            if (computedVdot > vdotScore) {
                lowSeconds = midSeconds;
            } else {
                highSeconds = midSeconds;
            }
        }

        return (lowSeconds + highSeconds) / 2.0;
    }
}
