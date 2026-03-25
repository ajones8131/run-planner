package com.runplanner.vdot;

public final class VdotConstants {

    private VdotConstants() {}

    public static final double MIN_VDOT = 30.0;
    public static final double MAX_VDOT = 85.0;

    // Oxygen cost formula coefficients (curve-fit to Daniels published VDOT tables)
    public static final double O2_INTERCEPT = -4.60;
    public static final double O2_LINEAR = 0.67921;
    public static final double O2_QUADRATIC = -0.001998;

    // %VO2max formula coefficients
    public static final double PCT_BASE = 0.8;
    public static final double PCT_COEFF_1 = 0.1894393;
    public static final double PCT_EXP_1 = -0.012778;
    public static final double PCT_COEFF_2 = 0.2989558;
    public static final double PCT_EXP_2 = -0.1932605;

    // Standard race distances in meters
    public static final double DISTANCE_5K = 5_000.0;
    public static final double DISTANCE_10K = 10_000.0;
    public static final double DISTANCE_HALF_MARATHON = 21_097.0;
    public static final double DISTANCE_MARATHON = 42_195.0;

    // VDOT history flagging threshold
    public static final double FLAGGING_THRESHOLD = 5.0;
}
