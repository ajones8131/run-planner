package com.runplanner.vdot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainingZone {

    E("Easy", "Aerobic base, recovery"),
    M("Marathon", "Race-specific aerobic work"),
    T("Threshold", "Lactate threshold improvement"),
    I("Interval", "VO2 max development"),
    R("Repetition", "Speed and economy");

    private final String displayName;
    private final String purpose;
}
