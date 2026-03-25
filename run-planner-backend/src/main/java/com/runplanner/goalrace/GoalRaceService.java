package com.runplanner.goalrace;

import com.runplanner.goalrace.dto.CreateGoalRaceRequest;
import com.runplanner.goalrace.dto.GoalRaceResponse;
import com.runplanner.goalrace.dto.UpdateGoalRaceRequest;
import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalRaceService {

    private final GoalRaceRepository goalRaceRepository;

    @Transactional
    public GoalRaceResponse create(User user, CreateGoalRaceRequest request) {
        var race = GoalRace.builder()
            .user(user)
            .distanceMeters(request.distanceMeters())
            .distanceLabel(request.distanceLabel())
            .raceDate(request.raceDate())
            .goalFinishSeconds(request.goalFinishSeconds())
            .build();
        return GoalRaceResponse.from(goalRaceRepository.save(race));
    }

    public List<GoalRaceResponse> findAll(User user) {
        return goalRaceRepository.findAllByUserOrderByRaceDateDesc(user)
            .stream()
            .map(GoalRaceResponse::from)
            .toList();
    }

    @Transactional
    public GoalRaceResponse update(User user, UUID id, UpdateGoalRaceRequest request) {
        var race = goalRaceRepository.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal race not found"));
        if (request.raceDate() != null) race.setRaceDate(request.raceDate());
        if (request.goalFinishSeconds() != null) race.setGoalFinishSeconds(request.goalFinishSeconds());
        if (request.status() != null) race.setStatus(request.status());
        return GoalRaceResponse.from(goalRaceRepository.save(race));
    }
}
