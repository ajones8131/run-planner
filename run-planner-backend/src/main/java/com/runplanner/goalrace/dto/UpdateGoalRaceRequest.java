package com.runplanner.goalrace.dto;

import com.runplanner.goalrace.GoalRaceStatus;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record UpdateGoalRaceRequest(
    LocalDate raceDate,
    @Positive Integer goalFinishSeconds,
    GoalRaceStatus status
) {}
