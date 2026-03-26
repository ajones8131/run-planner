package com.runplanner.adjustment;

public record AdjustmentDecision(AdjustmentType type, String reason) {

    public static AdjustmentDecision none() {
        return new AdjustmentDecision(AdjustmentType.NONE, null);
    }
}
