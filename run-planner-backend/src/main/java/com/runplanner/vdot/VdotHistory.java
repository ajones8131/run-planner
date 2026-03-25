package com.runplanner.vdot;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vdot_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VdotHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "triggering_workout_id")
    private UUID triggeringWorkoutId;

    @Column(name = "triggering_snapshot_id")
    private UUID triggeringSnapshotId;

    @Column(name = "previous_vdot", nullable = false)
    private double previousVdot;

    @Column(name = "new_vdot", nullable = false)
    private double newVdot;

    @Builder.Default
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    @Builder.Default
    @Column(nullable = false)
    private boolean flagged = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean accepted = true;
}
