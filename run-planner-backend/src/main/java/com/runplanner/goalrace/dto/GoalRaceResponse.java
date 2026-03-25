package com.runplanner.goalrace.dto;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.goalrace.GoalRaceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record GoalRaceResponse(
    UUID id,
    Integer distanceMeters,
    String distanceLabel,
    LocalDate raceDate,
    Integer goalFinishSeconds,
    GoalRaceStatus status
) {
    public static GoalRaceResponse from(GoalRace race) {
        return new GoalRaceResponse(race.getId(), race.getDistanceMeters(), race.getDistanceLabel(),
            race.getRaceDate(), race.getGoalFinishSeconds(), race.getStatus());
    }
}
