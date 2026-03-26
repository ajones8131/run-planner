package com.runplanner.plan;

import com.runplanner.goalrace.GoalRace;
import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_race_id", nullable = false)
    private GoalRace goalRace;

    @Column(name = "goal_race_id", insertable = false, updatable = false)
    private UUID goalRaceId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrainingPlanStatus status = TrainingPlanStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private int revision = 1;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    private Double lastAdjustmentVdot;
}
