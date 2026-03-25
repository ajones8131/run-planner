package com.runplanner.match;

public final class MatchConstants {

    private MatchConstants() {}

    public static final double DISTANCE_WEIGHT = 0.4;
    public static final double PACE_WEIGHT = 0.4;
    public static final double HR_WEIGHT = 0.2;

    public static final double[] HR_ZONE_LOWER_BOUNDS = {0.65, 0.80, 0.86, 0.91, 0.96};
    public static final double[] HR_ZONE_UPPER_BOUNDS = {0.79, 0.85, 0.90, 0.95, 1.00};

    public static final double HR_ZONE_MATCH = 1.0;
    public static final double HR_ZONE_ADJACENT = 0.5;
    public static final double HR_ZONE_FAR = 0.0;

    public static final double DEFAULT_FACTOR = 1.0;

    public static final int MATCH_WINDOW_DAYS = 1;
}
