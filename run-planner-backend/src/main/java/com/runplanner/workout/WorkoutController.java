package com.runplanner.workout;

import com.runplanner.user.User;
import com.runplanner.workout.dto.WorkoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workouts")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @GetMapping
    public List<WorkoutResponse> list(@AuthenticationPrincipal User user,
                                      @RequestParam(required = false) Instant since) {
        return workoutService.findAll(user, since);
    }

    @GetMapping("/{id}")
    public WorkoutResponse get(@AuthenticationPrincipal User user,
                               @PathVariable UUID id) {
        return workoutService.findById(user, id);
    }
}
