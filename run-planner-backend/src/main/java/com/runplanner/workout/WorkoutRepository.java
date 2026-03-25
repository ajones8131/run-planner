package com.runplanner.workout;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    List<Workout> findAllByUserAndStartedAtAfterOrderByStartedAtDesc(User user, Instant since);

    List<Workout> findAllByUserOrderByStartedAtDesc(User user);

    Optional<Workout> findByIdAndUser(UUID id, User user);

    boolean existsBySourceAndSourceId(String source, String sourceId);
}
