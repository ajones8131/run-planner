package com.runplanner.health;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthSnapshotRepository extends JpaRepository<HealthSnapshot, UUID> {

    Optional<HealthSnapshot> findFirstByUserAndVo2maxEstimateIsNotNullOrderByRecordedAtDesc(User user);

    List<HealthSnapshot> findAllByUserOrderByRecordedAtDesc(User user);
}
