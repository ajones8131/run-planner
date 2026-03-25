package com.runplanner.vdot;

import com.runplanner.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VdotHistoryRepository extends JpaRepository<VdotHistory, UUID> {

    Optional<VdotHistory> findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(User user);

    List<VdotHistory> findAllByUserOrderByCalculatedAtDesc(User user);
}
