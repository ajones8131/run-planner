package com.runplanner.plan;

import java.util.EnumMap;
import java.util.Map;

public final class PlanConstants {

    private PlanConstants() {}

    // Phase splits (must sum to 1.0)
    public static final double BASE_PHASE_PCT = 0.25;
    public static final double QUALITY_PHASE_PCT = 0.25;
    public static final double PEAK_PHASE_PCT = 0.25;
    public static final double TAPER_PHASE_PCT = 0.25;

    // Minimum weeks per phase for compressed plans
    public static final int MIN_WEEKS_PER_PHASE = 1;

    // Minimum total weeks for a 4-phase plan
    public static final int MIN_WEEKS_FULL_PLAN = 4;

    // Weekly templates: index 0=Monday(1) through 6=Sunday(7)
    public static final Map<TrainingPhase, WorkoutType[]> WEEKLY_TEMPLATES;

    static {
        WEEKLY_TEMPLATES = new EnumMap<>(TrainingPhase.class);
        WEEKLY_TEMPLATES.put(TrainingPhase.BASE, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.EASY, WorkoutType.EASY,
                WorkoutType.EASY, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.QUALITY, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.THRESHOLD, WorkoutType.EASY,
                WorkoutType.INTERVAL, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.PEAK, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.INTERVAL, WorkoutType.EASY,
                WorkoutType.THRESHOLD, WorkoutType.REST, WorkoutType.LONG, WorkoutType.MARATHON
        });
        WEEKLY_TEMPLATES.put(TrainingPhase.TAPER, new WorkoutType[]{
                WorkoutType.REST, WorkoutType.EASY, WorkoutType.EASY,
                WorkoutType.THRESHOLD, WorkoutType.REST, WorkoutType.LONG, WorkoutType.EASY
        });
    }

    // Distance distribution within a week
    public static final double LONG_RUN_PCT = 0.27;
    public static final double QUALITY_SESSION_PCT = 0.17;
    public static final double REST_DISTANCE = 0.0;

    // Mileage progression factors (multiplied against weekly target)
    public static final double BASE_START_FACTOR = 0.70;
    public static final double BASE_END_FACTOR = 1.00;
    public static final double QUALITY_FACTOR = 1.00;
    public static final double PEAK_START_FACTOR = 1.00;
    public static final double PEAK_END_FACTOR = 1.05;
    public static final double TAPER_START_FACTOR = 1.00;
    public static final double TAPER_END_FACTOR = 0.60;

    // Base weekly mileage (km) by VDOT range
    public static final double MILEAGE_BASE_KM = 30.0;
    public static final double MILEAGE_PER_VDOT = 1.0;
}
