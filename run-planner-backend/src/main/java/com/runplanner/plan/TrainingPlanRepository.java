package com.runplanner.plan;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    Optional<TrainingPlan> findByUserAndStatus(User user, TrainingPlanStatus status);

    List<TrainingPlan> findAllByUserOrderByCreatedAtDesc(User user);

    Optional<TrainingPlan> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndStatus(User user, TrainingPlanStatus status);
}
