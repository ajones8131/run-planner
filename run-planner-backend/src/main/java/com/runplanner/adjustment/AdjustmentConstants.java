package com.runplanner.adjustment;

public final class AdjustmentConstants {

    private AdjustmentConstants() {}

    public static final double MAJOR_LOW_COMPLIANCE_THRESHOLD = 0.6;
    public static final int MAJOR_CONSECUTIVE_COUNT = 2;
    public static final double VDOT_CHANGE_THRESHOLD = 2.0;
    public static final double OVER_PERFORMANCE_PACE_FACTOR = 0.90;

    public static final double MINOR_COMPLIANCE_LOW = 0.6;
    public static final double MINOR_COMPLIANCE_HIGH = 0.75;
    public static final int MINOR_WINDOW_SIZE = 5;
    public static final int MINOR_TRIGGER_COUNT = 3;

    public static final int MISSED_LONG_RUN_WINDOW_DAYS = 7;
    public static final int RECENT_MATCH_WINDOW_DAYS = 14;
}
