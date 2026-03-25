package com.runplanner.workout;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String source;

    private String sourceId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false)
    private Integer durationSeconds;

    private Integer avgHr;

    private Integer maxHr;

    private Double elevationGain;
}
