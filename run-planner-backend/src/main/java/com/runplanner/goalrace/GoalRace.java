package com.runplanner.goalrace;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goal_races")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer distanceMeters;

    @Column(nullable = false)
    private String distanceLabel;

    @Column(nullable = false)
    private LocalDate raceDate;

    private Integer goalFinishSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoalRaceStatus status = GoalRaceStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
