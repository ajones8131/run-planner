package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goal-races")
@RequiredArgsConstructor
public class GoalRaceController {

    private final GoalRaceService goalRaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalRaceResponse create(@AuthenticationPrincipal User user,
                                   @Valid @RequestBody CreateGoalRaceRequest request) {
        return goalRaceService.create(user, request);
    }

    @GetMapping
    public List<GoalRaceResponse> list(@AuthenticationPrincipal User user) {
        return goalRaceService.findAll(user);
    }

    @PatchMapping("/{id}")
    public GoalRaceResponse update(@AuthenticationPrincipal User user,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody UpdateGoalRaceRequest request) {
        return goalRaceService.update(user, id, request);
    }
}
