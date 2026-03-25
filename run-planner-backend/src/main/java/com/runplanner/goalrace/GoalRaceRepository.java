package com.runplanner.goalrace;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRaceRepository extends JpaRepository<GoalRace, UUID> {
    List<GoalRace> findAllByUserOrderByRaceDateDesc(User user);
    Optional<GoalRace> findByIdAndUser(UUID id, User user);
    List<GoalRace> findAllByUserAndStatus(User user, GoalRaceStatus status);
}
