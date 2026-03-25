package com.runplanner.user.dto;

import com.runplanner.user.Units;

import java.time.LocalDate;

public record UpdateProfileRequest(
    String name,
    LocalDate dateOfBirth,
    Integer maxHr,
    Units preferredUnits
) {}
