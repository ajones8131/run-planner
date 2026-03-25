package com.runplanner.goalrace.dto;

import com.runplanner.goalrace.GoalRaceStatus;

import java.time.LocalDate;

public record UpdateGoalRaceRequest(
    LocalDate raceDate,
    Integer goalFinishSeconds,
    GoalRaceStatus status
) {}
