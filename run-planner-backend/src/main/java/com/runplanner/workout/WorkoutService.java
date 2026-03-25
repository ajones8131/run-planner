package com.runplanner.workout;

import com.runplanner.user.User;
import com.runplanner.vdot.VdotRecalculationService;
import com.runplanner.workout.dto.WorkoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final VdotRecalculationService vdotRecalculationService;

    @Transactional
    public Workout save(Workout workout) {
        Workout saved = workoutRepository.save(workout);
        vdotRecalculationService.evaluate(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WorkoutResponse> findAll(User user, Instant since) {
        var workouts = (since != null)
                ? workoutRepository.findAllByUserAndStartedAtAfterOrderByStartedAtDesc(user, since)
                : workoutRepository.findAllByUserOrderByStartedAtDesc(user);
        return workouts.stream().map(WorkoutResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public WorkoutResponse findById(User user, UUID id) {
        return workoutRepository.findByIdAndUser(id, user)
                .map(WorkoutResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Workout not found"));
    }

    @Transactional(readOnly = true)
    public boolean existsBySourceAndSourceId(String source, String sourceId) {
        return workoutRepository.existsBySourceAndSourceId(source, sourceId);
    }
}
