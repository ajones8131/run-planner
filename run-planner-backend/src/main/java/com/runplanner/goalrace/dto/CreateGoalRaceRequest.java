package com.runplanner.goalrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateGoalRaceRequest(
    @NotNull @Positive Integer distanceMeters,
    @NotBlank String distanceLabel,
    @NotNull LocalDate raceDate,
    Integer goalFinishSeconds
) {}
