package com.runplanner.plan;

import com.runplanner.plan.dto.CreatePlanRequest;
import com.runplanner.plan.dto.PlannedWorkoutResponse;
import com.runplanner.plan.dto.TrainingPlanResponse;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingPlanResponse create(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody CreatePlanRequest request) {
        return trainingPlanService.generate(user, request.goalRaceId());
    }

    @GetMapping
    public List<TrainingPlanResponse> list(@AuthenticationPrincipal User user) {
        return trainingPlanService.findAll(user);
    }

    @GetMapping("/active")
    public TrainingPlanResponse getActive(@AuthenticationPrincipal User user) {
        return trainingPlanService.findActive(user);
    }

    @GetMapping("/{id}")
    public TrainingPlanResponse getById(@AuthenticationPrincipal User user,
                                        @PathVariable UUID id) {
        return trainingPlanService.findById(user, id);
    }

    @GetMapping("/{id}/workouts")
    public List<PlannedWorkoutResponse> getWorkouts(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return trainingPlanService.findPlannedWorkouts(user, id, from, to);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal User user,
                        @PathVariable UUID id) {
        trainingPlanService.archive(user, id);
    }
}
