package com.runplanner.plan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePlanRequest(@NotNull UUID goalRaceId) {}
