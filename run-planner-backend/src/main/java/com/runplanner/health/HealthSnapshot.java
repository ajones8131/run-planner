package com.runplanner.health;

import com.runplanner.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "health_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double vo2maxEstimate;

    private Integer restingHr;

    @Column(nullable = false)
    private Instant recordedAt;
}
