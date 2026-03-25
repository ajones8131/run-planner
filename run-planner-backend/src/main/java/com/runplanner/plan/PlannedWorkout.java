package com.runplanner.plan;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "planned_workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannedWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @Column(nullable = false)
    private int weekNumber;

    @Column(nullable = false)
    private int dayOfWeek;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkoutType workoutType;

    @Column(nullable = false)
    private double targetDistanceMeters;

    private Double targetPaceMinPerKm;

    private Double targetPaceMaxPerKm;

    private Integer targetHrZone;

    private String notes;

    @Column(nullable = false)
    private LocalDate originalScheduledDate;

    @Column(nullable = false)
    @Builder.Default
    private int planRevision = 1;
}
