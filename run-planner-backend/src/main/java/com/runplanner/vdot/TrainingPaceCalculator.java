package com.runplanner.vdot;

import java.util.EnumMap;
import java.util.Map;

import static com.runplanner.vdot.VdotConstants.*;

public class TrainingPaceCalculator {

    private static final Map<TrainingZone, double[]> ZONE_PCT_VO2MAX = Map.of(
            TrainingZone.E, new double[]{0.59, 0.74},
            TrainingZone.M, new double[]{0.75, 0.84},
            TrainingZone.T, new double[]{0.83, 0.88},
            TrainingZone.I, new double[]{0.95, 1.00},
            TrainingZone.R, new double[]{1.05, 1.10}
    );

    /**
     * Calculates training pace ranges for all five zones from a VDOT score.
     *
     * @param vdotScore the VDOT score (must be between MIN_VDOT and MAX_VDOT)
     * @return map of training zone to pace range (min/km)
     */
    public Map<TrainingZone, PaceRange> calculate(double vdotScore) {
        if (vdotScore < MIN_VDOT || vdotScore > MAX_VDOT) {
            throw new IllegalArgumentException(
                    "VDOT must be between " + MIN_VDOT + " and " + MAX_VDOT);
        }

        // VO2max in ml/kg/min equals the VDOT score
        double vo2max = vdotScore;

        Map<TrainingZone, PaceRange> result = new EnumMap<>(TrainingZone.class);

        for (TrainingZone zone : TrainingZone.values()) {
            double[] pctRange = ZONE_PCT_VO2MAX.get(zone);
            // Higher %VO2max → faster pace (lower min/km) → min pace
            double fastPace = velocityToPace(oxygenCostToVelocity(vo2max * pctRange[1]));
            // Lower %VO2max → slower pace (higher min/km) → max pace
            double slowPace = velocityToPace(oxygenCostToVelocity(vo2max * pctRange[0]));
            result.put(zone, new PaceRange(fastPace, slowPace));
        }

        return result;
    }

    /**
     * Converts oxygen cost (ml/kg/min) to running velocity (meters/min)
     * by inverting the oxygen cost formula using the quadratic formula.
     */
    private double oxygenCostToVelocity(double oxygenCost) {
        // oxygenCost = O2_INTERCEPT + O2_LINEAR * v + O2_QUADRATIC * v²
        // Rearranged: O2_QUADRATIC * v² + O2_LINEAR * v + (O2_INTERCEPT - oxygenCost) = 0
        double a = O2_QUADRATIC;
        double b = O2_LINEAR;
        double c = O2_INTERCEPT - oxygenCost;

        double discriminant = b * b - 4 * a * c;
        // Take the positive root
        return (-b + Math.sqrt(discriminant)) / (2 * a);
    }

    /**
     * Converts velocity (meters/min) to pace (min/km).
     */
    private double velocityToPace(double velocityMetersPerMin) {
        return 1000.0 / velocityMetersPerMin;
    }
}
