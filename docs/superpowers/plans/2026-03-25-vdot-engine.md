# VDOT Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a pure math VDOT engine with persistence and REST endpoints for managing VDOT history.

**Architecture:** Pure math classes (`VdotCalculator`, `TrainingPaceCalculator`) in a `vdot` package with no Spring dependencies. Separate `VdotHistory` entity, repository, service, and controller follow the same layered patterns as `goalrace`. Flyway migration V4 creates the `vdot_history` table.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, PostgreSQL, Flyway, JUnit 5, Mockito, AssertJ, MockMvc, Lombok

**Spec:** `docs/superpowers/specs/2026-03-25-vdot-engine-design.md`

---

### Task 1: VdotConstants and PaceRange

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/TrainingZone.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/PaceRange.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotConstants.java`

- [ ] **Step 1: Create TrainingZone enum**

```java
package com.runplanner.vdot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrainingZone {

    E("Easy", "Aerobic base, recovery"),
    M("Marathon", "Race-specific aerobic work"),
    T("Threshold", "Lactate threshold improvement"),
    I("Interval", "VO2 max development"),
    R("Repetition", "Speed and economy");

    private final String displayName;
    private final String purpose;
}
```

- [ ] **Step 2: Create PaceRange record**

```java
package com.runplanner.vdot;

public record PaceRange(double minPaceMinPerKm, double maxPaceMinPerKm) {

    public PaceRange {
        if (minPaceMinPerKm <= 0 || maxPaceMinPerKm <= 0) {
            throw new IllegalArgumentException("Pace values must be positive");
        }
        if (minPaceMinPerKm > maxPaceMinPerKm) {
            throw new IllegalArgumentException(
                    "minPace (faster) must be <= maxPace (slower)");
        }
    }
}
```

- [ ] **Step 3: Create VdotConstants**

```java
package com.runplanner.vdot;

public final class VdotConstants {

    private VdotConstants() {}

    public static final double MIN_VDOT = 30.0;
    public static final double MAX_VDOT = 85.0;

    // Oxygen cost formula coefficients
    public static final double O2_INTERCEPT = -4.60;
    public static final double O2_LINEAR = 0.182258;
    public static final double O2_QUADRATIC = 0.000104;

    // %VO2max formula coefficients
    public static final double PCT_BASE = 0.8;
    public static final double PCT_COEFF_1 = 0.1894393;
    public static final double PCT_EXP_1 = -0.012778;
    public static final double PCT_COEFF_2 = 0.2989558;
    public static final double PCT_EXP_2 = -0.1932605;

    // Standard race distances in meters
    public static final double DISTANCE_5K = 5_000.0;
    public static final double DISTANCE_10K = 10_000.0;
    public static final double DISTANCE_HALF_MARATHON = 21_097.0;
    public static final double DISTANCE_MARATHON = 42_195.0;

    // VDOT history flagging threshold
    public static final double FLAGGING_THRESHOLD = 5.0;
}
```

- [ ] **Step 4: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/
git commit -m "feat(vdot): add TrainingZone, PaceRange, and VdotConstants"
```

---

### Task 2: VdotCalculator — calculateVdot

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/vdot/VdotCalculatorTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotCalculator.java`

- [ ] **Step 1: Write failing tests for calculateVdot**

```java
package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VdotCalculatorTest {

    private final VdotCalculator calculator = new VdotCalculator();

    // --- calculateVdot ---

    @Test
    void calculateVdot_5kIn20Minutes_returnsExpectedVdot() {
        // 5K in 20:00 → ~42.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(5_000, 20 * 60);
        assertThat(vdot).isCloseTo(42.2, within(0.5));
    }

    @Test
    void calculateVdot_marathonIn3Hours_returnsExpectedVdot() {
        // Marathon in 3:00:00 → ~54.6 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(42_195, 3 * 3600);
        assertThat(vdot).isCloseTo(54.6, within(0.5));
    }

    @Test
    void calculateVdot_10kIn40Minutes_returnsExpectedVdot() {
        // 10K in 40:00 → ~44.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(10_000, 40 * 60);
        assertThat(vdot).isCloseTo(44.2, within(0.5));
    }

    @Test
    void calculateVdot_halfMarathonIn90Minutes_returnsExpectedVdot() {
        // Half marathon in 1:30:00 → ~52.2 VDOT (Daniels table)
        double vdot = calculator.calculateVdot(21_097, 90 * 60);
        assertThat(vdot).isCloseTo(52.2, within(0.5));
    }

    @Test
    void calculateVdot_negativeDistance_throwsException() {
        assertThatThrownBy(() -> calculator.calculateVdot(-100, 600))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculateVdot_zeroTime_throwsException() {
        assertThatThrownBy(() -> calculator.calculateVdot(5_000, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotCalculatorTest test`
Expected: Compilation failure — `VdotCalculator` class not found

- [ ] **Step 3: Implement VdotCalculator.calculateVdot**

```java
package com.runplanner.vdot;

import static com.runplanner.vdot.VdotConstants.*;

public class VdotCalculator {

    /**
     * Calculates VDOT score from a race performance.
     *
     * @param distanceMeters race distance in meters
     * @param timeSeconds    race finish time in seconds
     * @return VDOT score
     */
    public double calculateVdot(double distanceMeters, double timeSeconds) {
        if (distanceMeters <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (timeSeconds <= 0) {
            throw new IllegalArgumentException("Time must be positive");
        }

        double timeMinutes = timeSeconds / 60.0;
        double velocity = distanceMeters / timeMinutes; // meters per minute

        double oxygenCost = O2_INTERCEPT
                + O2_LINEAR * velocity
                + O2_QUADRATIC * velocity * velocity;

        double pctVO2max = PCT_BASE
                + PCT_COEFF_1 * Math.exp(PCT_EXP_1 * timeMinutes)
                + PCT_COEFF_2 * Math.exp(PCT_EXP_2 * timeMinutes);

        return oxygenCost / pctVO2max;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotCalculatorTest test`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotCalculator.java \
       run-planner-backend/src/test/java/com/runplanner/vdot/VdotCalculatorTest.java
git commit -m "feat(vdot): implement calculateVdot with tests"
```

---

### Task 3: VdotCalculator — predictRaceTime

**Files:**
- Modify: `run-planner-backend/src/test/java/com/runplanner/vdot/VdotCalculatorTest.java`
- Modify: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotCalculator.java`

- [ ] **Step 1: Add failing tests for predictRaceTime**

Add to `VdotCalculatorTest.java`:

```java
// --- predictRaceTime ---

@Test
void predictRaceTime_vdot50For5k_returnsExpectedTime() {
    // VDOT 50 → 5K in ~20:30 (Daniels table, ~1230 seconds)
    double time = calculator.predictRaceTime(50.0, 5_000);
    assertThat(time).isCloseTo(1230, within(15.0));
}

@Test
void predictRaceTime_vdot50ForMarathon_returnsExpectedTime() {
    // VDOT 50 → marathon in ~3:20:00 (Daniels table, ~12000 seconds)
    double time = calculator.predictRaceTime(50.0, 42_195);
    assertThat(time).isCloseTo(12000, within(60.0));
}

@Test
void predictRaceTime_roundTrip_returnsOriginalTime() {
    // Calculate VDOT from a known race, then predict time for same distance
    double originalTime = 20 * 60; // 20:00 5K
    double vdot = calculator.calculateVdot(5_000, originalTime);
    double predictedTime = calculator.predictRaceTime(vdot, 5_000);
    assertThat(predictedTime).isCloseTo(originalTime, within(1.0));
}

@Test
void predictRaceTime_vdotBelowRange_throwsException() {
    assertThatThrownBy(() -> calculator.predictRaceTime(20.0, 5_000))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void predictRaceTime_vdotAboveRange_throwsException() {
    assertThatThrownBy(() -> calculator.predictRaceTime(90.0, 5_000))
            .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Run tests to verify the new tests fail**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotCalculatorTest test`
Expected: Compilation failure — `predictRaceTime` method not found

- [ ] **Step 3: Implement predictRaceTime using binary search**

Add to `VdotCalculator.java`:

```java
/**
 * Predicts race finish time for a given VDOT and distance using binary search.
 *
 * @param vdotScore      the VDOT score (must be between MIN_VDOT and MAX_VDOT)
 * @param distanceMeters race distance in meters
 * @return predicted finish time in seconds
 */
public double predictRaceTime(double vdotScore, double distanceMeters) {
    if (vdotScore < MIN_VDOT || vdotScore > MAX_VDOT) {
        throw new IllegalArgumentException(
                "VDOT must be between " + MIN_VDOT + " and " + MAX_VDOT);
    }
    if (distanceMeters <= 0) {
        throw new IllegalArgumentException("Distance must be positive");
    }

    // Binary search: find time where calculateVdot(distance, time) == vdotScore
    // Lower bound: very fast (1 min/km pace)
    // Upper bound: very slow (15 min/km pace)
    double lowSeconds = distanceMeters / 1000.0 * 60;   // 1 min/km
    double highSeconds = distanceMeters / 1000.0 * 900;  // 15 min/km

    for (int i = 0; i < 100; i++) {
        double midSeconds = (lowSeconds + highSeconds) / 2.0;
        double computedVdot = calculateVdot(distanceMeters, midSeconds);

        if (Math.abs(computedVdot - vdotScore) < 0.001) {
            return midSeconds;
        }

        // Faster time → higher VDOT, so if computed > target, slow down
        if (computedVdot > vdotScore) {
            lowSeconds = midSeconds;
        } else {
            highSeconds = midSeconds;
        }
    }

    return (lowSeconds + highSeconds) / 2.0;
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotCalculatorTest test`
Expected: All 11 tests PASS

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotCalculator.java \
       run-planner-backend/src/test/java/com/runplanner/vdot/VdotCalculatorTest.java
git commit -m "feat(vdot): implement predictRaceTime with binary search and tests"
```

---

### Task 4: TrainingPaceCalculator

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/vdot/TrainingPaceCalculatorTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/TrainingPaceCalculator.java`

- [ ] **Step 1: Write failing tests**

```java
package com.runplanner.vdot;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TrainingPaceCalculatorTest {

    private final TrainingPaceCalculator calculator = new TrainingPaceCalculator();

    @Test
    void calculate_allFiveZonesPresent() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        assertThat(paces).containsKeys(TrainingZone.values());
    }

    @Test
    void calculate_minPaceFasterThanMaxPaceForAllZones() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        paces.forEach((zone, range) ->
                assertThat(range.minPaceMinPerKm())
                        .as("Zone %s: min (faster) should be <= max (slower)", zone)
                        .isLessThanOrEqualTo(range.maxPaceMinPerKm()));
    }

    @Test
    void calculate_easyIsSlowestAndRepIsFastest() {
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        // Easy max (slowest boundary) should be slower than Rep min (fastest boundary)
        assertThat(paces.get(TrainingZone.E).maxPaceMinPerKm())
                .isGreaterThan(paces.get(TrainingZone.R).minPaceMinPerKm());
    }

    @Test
    void calculate_higherVdotProducesFasterPaces() {
        Map<TrainingZone, PaceRange> lowVdot = calculator.calculate(40.0);
        Map<TrainingZone, PaceRange> highVdot = calculator.calculate(60.0);

        for (TrainingZone zone : TrainingZone.values()) {
            assertThat(highVdot.get(zone).minPaceMinPerKm())
                    .as("Zone %s: higher VDOT should produce faster min pace", zone)
                    .isLessThan(lowVdot.get(zone).minPaceMinPerKm());
            assertThat(highVdot.get(zone).maxPaceMinPerKm())
                    .as("Zone %s: higher VDOT should produce faster max pace", zone)
                    .isLessThan(lowVdot.get(zone).maxPaceMinPerKm());
        }
    }

    @Test
    void calculate_vdot50EasyPace_matchesDanielsTable() {
        // VDOT 50 → Easy pace ~5:30-6:08/km (Daniels table)
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        PaceRange easy = paces.get(TrainingZone.E);
        // Allow ±0.05 min/km (~3 sec/km) tolerance
        assertThat(easy.minPaceMinPerKm()).isCloseTo(5.5, within(0.15));
        assertThat(easy.maxPaceMinPerKm()).isCloseTo(6.13, within(0.15));
    }

    @Test
    void calculate_vdot50ThresholdPace_matchesDanielsTable() {
        // VDOT 50 → Threshold pace ~4:30-4:36/km (Daniels table)
        Map<TrainingZone, PaceRange> paces = calculator.calculate(50.0);
        PaceRange threshold = paces.get(TrainingZone.T);
        assertThat(threshold.minPaceMinPerKm()).isCloseTo(4.50, within(0.15));
        assertThat(threshold.maxPaceMinPerKm()).isCloseTo(4.60, within(0.15));
    }

    @Test
    void calculate_belowMinVdot_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(29.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculate_aboveMaxVdot_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(86.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=TrainingPaceCalculatorTest test`
Expected: Compilation failure — `TrainingPaceCalculator` class not found

- [ ] **Step 3: Implement TrainingPaceCalculator**

```java
package com.runplanner.vdot;

import java.util.EnumMap;
import java.util.Map;

import static com.runplanner.vdot.VdotConstants.*;

public class TrainingPaceCalculator {

    private static final Map<TrainingZone, double[]> ZONE_PCT_VO2MAX = Map.of(
            TrainingZone.E, new double[]{0.59, 0.74},
            TrainingZone.M, new double[]{0.75, 0.84},
            TrainingZone.T, new double[]{0.83, 0.88},
            TrainingZone.I, new double[]{0.95, 1.00},
            TrainingZone.R, new double[]{1.05, 1.10}
    );

    /**
     * Calculates training pace ranges for all five zones from a VDOT score.
     *
     * @param vdotScore the VDOT score (must be between MIN_VDOT and MAX_VDOT)
     * @return map of training zone to pace range (min/km)
     */
    public Map<TrainingZone, PaceRange> calculate(double vdotScore) {
        if (vdotScore < MIN_VDOT || vdotScore > MAX_VDOT) {
            throw new IllegalArgumentException(
                    "VDOT must be between " + MIN_VDOT + " and " + MAX_VDOT);
        }

        // VO2max in ml/kg/min equals the VDOT score
        double vo2max = vdotScore;

        Map<TrainingZone, PaceRange> result = new EnumMap<>(TrainingZone.class);

        for (TrainingZone zone : TrainingZone.values()) {
            double[] pctRange = ZONE_PCT_VO2MAX.get(zone);
            // Higher %VO2max → faster pace (lower min/km) → min pace
            double fastPace = velocityToPace(oxygenCostToVelocity(vo2max * pctRange[1]));
            // Lower %VO2max → slower pace (higher min/km) → max pace
            double slowPace = velocityToPace(oxygenCostToVelocity(vo2max * pctRange[0]));
            result.put(zone, new PaceRange(fastPace, slowPace));
        }

        return result;
    }

    /**
     * Converts oxygen cost (ml/kg/min) to running velocity (meters/min)
     * by inverting the oxygen cost formula using the quadratic formula.
     */
    private double oxygenCostToVelocity(double oxygenCost) {
        // oxygenCost = O2_INTERCEPT + O2_LINEAR * v + O2_QUADRATIC * v²
        // Rearranged: O2_QUADRATIC * v² + O2_LINEAR * v + (O2_INTERCEPT - oxygenCost) = 0
        double a = O2_QUADRATIC;
        double b = O2_LINEAR;
        double c = O2_INTERCEPT - oxygenCost;

        double discriminant = b * b - 4 * a * c;
        // Take the positive root
        return (-b + Math.sqrt(discriminant)) / (2 * a);
    }

    /**
     * Converts velocity (meters/min) to pace (min/km).
     */
    private double velocityToPace(double velocityMetersPerMin) {
        return 1000.0 / velocityMetersPerMin;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=TrainingPaceCalculatorTest test`
Expected: All 8 tests PASS

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/TrainingPaceCalculator.java \
       run-planner-backend/src/test/java/com/runplanner/vdot/TrainingPaceCalculatorTest.java
git commit -m "feat(vdot): implement TrainingPaceCalculator with tests"
```

---

### Task 5: VdotScore record

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotScore.java`

- [ ] **Step 1: Create VdotScore record**

```java
package com.runplanner.vdot;

import java.util.Map;

public record VdotScore(double score, Map<TrainingZone, PaceRange> trainingPaces) {

    public VdotScore {
        if (score < VdotConstants.MIN_VDOT || score > VdotConstants.MAX_VDOT) {
            throw new IllegalArgumentException(
                    "VDOT score must be between " + VdotConstants.MIN_VDOT
                    + " and " + VdotConstants.MAX_VDOT);
        }
        if (trainingPaces == null || trainingPaces.size() != TrainingZone.values().length) {
            throw new IllegalArgumentException("Training paces must include all zones");
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotScore.java
git commit -m "feat(vdot): add VdotScore convenience record"
```

---

### Task 6: Flyway migration V4

**Files:**
- Create: `run-planner-backend/src/main/resources/db/migration/V4__create_vdot_history.sql`

- [ ] **Step 1: Create migration**

```sql
CREATE TABLE vdot_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    triggering_workout_id   UUID,
    triggering_snapshot_id  UUID,
    previous_vdot           DOUBLE PRECISION NOT NULL,
    new_vdot                DOUBLE PRECISION NOT NULL,
    calculated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    flagged                 BOOLEAN NOT NULL DEFAULT FALSE,
    accepted                BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_single_trigger CHECK (
        (triggering_workout_id IS NOT NULL AND triggering_snapshot_id IS NULL) OR
        (triggering_workout_id IS NULL AND triggering_snapshot_id IS NOT NULL)
    )
);

CREATE INDEX idx_vdot_history_user_id ON vdot_history(user_id);
```

- [ ] **Step 2: Commit**

```bash
git add run-planner-backend/src/main/resources/db/migration/V4__create_vdot_history.sql
git commit -m "feat(vdot): add V4 migration for vdot_history table"
```

---

### Task 7: VdotHistory entity and repository

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistory.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryRepository.java`

- [ ] **Step 1: Create VdotHistory entity**

Follow the same pattern as `GoalRace.java`: `@Entity`, `@Table`, `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, UUID PK with `@GeneratedValue(strategy = GenerationType.UUID)`, `@ManyToOne(fetch = FetchType.LAZY)` for user FK.

```java
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
```

- [ ] **Step 2: Create VdotHistoryRepository**

```java
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
```

- [ ] **Step 3: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistory.java \
       run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryRepository.java
git commit -m "feat(vdot): add VdotHistory entity and repository"
```

---

### Task 8: VdotHistoryService

**Files:**
- Create: `run-planner-backend/src/test/java/com/runplanner/vdot/VdotHistoryServiceTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryService.java`

- [ ] **Step 1: Write failing tests**

Follow `GoalRaceServiceTest.java` patterns: `@ExtendWith(MockitoExtension.class)`, `@Mock` repository, `@InjectMocks` service, `User` helper method, `when(...).thenAnswer(...)`.

```java
package com.runplanner.vdot;

import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VdotHistoryServiceTest {

    @Mock
    private VdotHistoryRepository repository;

    @InjectMocks
    private VdotHistoryService service;

    private User user() {
        return User.builder().id(UUID.randomUUID()).email("test@test.com").build();
    }

    // --- recordCalculation ---

    @Test
    void recordCalculation_normalDelta_createsUnflaggedAcceptedEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();
        UUID workoutId = UUID.randomUUID();

        VdotHistory result = service.recordCalculation(user, 50.0, 52.0, workoutId, null);

        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getPreviousVdot()).isEqualTo(50.0);
        assertThat(result.getNewVdot()).isEqualTo(52.0);
        assertThat(result.getTriggeringWorkoutId()).isEqualTo(workoutId);
        assertThat(result.getTriggeringSnapshotId()).isNull();
        verify(repository).save(any());
    }

    @Test
    void recordCalculation_largeDelta_createsFlaggedUnacceptedEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 40.0, 50.0, UUID.randomUUID(), null);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void recordCalculation_exactlyFivePointDelta_notFlagged() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 50.0, 55.0, UUID.randomUUID(), null);

        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
    }

    @Test
    void recordCalculation_initialVdotWithZeroPrevious_createsEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        User user = user();

        VdotHistory result = service.recordCalculation(user, 0.0, 50.0, null, UUID.randomUUID());

        assertThat(result.getPreviousVdot()).isEqualTo(0.0);
        assertThat(result.getTriggeringSnapshotId()).isNotNull();
        assertThat(result.getTriggeringWorkoutId()).isNull();
        // Initial calculation with 0 previous: delta is 50, which is > 5,
        // but 0.0 previousVdot signals first entry — not flagged
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.isAccepted()).isTrue();
    }

    // --- getEffectiveVdot ---

    @Test
    void getEffectiveVdot_withAcceptedEntry_returnsNewVdot() {
        User user = user();
        VdotHistory entry = VdotHistory.builder()
                .user(user).newVdot(52.0).accepted(true).build();
        when(repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user))
                .thenReturn(Optional.of(entry));

        Optional<Double> result = service.getEffectiveVdot(user);

        assertThat(result).contains(52.0);
    }

    @Test
    void getEffectiveVdot_noHistory_returnsEmpty() {
        User user = user();
        when(repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user))
                .thenReturn(Optional.empty());

        Optional<Double> result = service.getEffectiveVdot(user);

        assertThat(result).isEmpty();
    }

    // --- acceptFlagged ---

    @Test
    void acceptFlagged_flaggedEntry_setsAcceptedTrue() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acceptFlagged(user, historyId);

        ArgumentCaptor<VdotHistory> captor = ArgumentCaptor.forClass(VdotHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isAccepted()).isTrue();
    }

    @Test
    void acceptFlagged_notFlagged_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(false).accepted(true).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptFlagged_differentUser_throwsException() {
        User user = user();
        User otherUser = User.builder().id(UUID.randomUUID()).email("other@test.com").build();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(otherUser).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void acceptFlagged_notFound_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        when(repository.findById(historyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- dismissFlagged ---

    @Test
    void dismissFlagged_flaggedEntry_keepsAcceptedFalse() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        service.dismissFlagged(user, historyId);

        // Entry is kept but not accepted — verify no save with accepted=true
        verify(repository, never()).save(any());
    }

    @Test
    void dismissFlagged_notFlagged_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(user).flagged(false).accepted(true).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void dismissFlagged_differentUser_throwsException() {
        User user = user();
        User otherUser = User.builder().id(UUID.randomUUID()).email("other@test.com").build();
        UUID historyId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(historyId).user(otherUser).flagged(true).accepted(false).build();
        when(repository.findById(historyId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void dismissFlagged_notFound_throwsException() {
        User user = user();
        UUID historyId = UUID.randomUUID();
        when(repository.findById(historyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dismissFlagged(user, historyId))
                .isInstanceOf(ResponseStatusException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotHistoryServiceTest test`
Expected: Compilation failure — `VdotHistoryService` class not found

- [ ] **Step 3: Implement VdotHistoryService**

```java
package com.runplanner.vdot;

import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.runplanner.vdot.VdotConstants.FLAGGING_THRESHOLD;

@Service
@RequiredArgsConstructor
public class VdotHistoryService {

    private final VdotHistoryRepository repository;

    @Transactional
    public VdotHistory recordCalculation(User user, double previousVdot, double newVdot,
                                         UUID triggeringWorkoutId, UUID triggeringSnapshotId) {
        boolean isInitial = previousVdot == 0.0;
        boolean shouldFlag = !isInitial && Math.abs(newVdot - previousVdot) > FLAGGING_THRESHOLD;

        var entry = VdotHistory.builder()
                .user(user)
                .triggeringWorkoutId(triggeringWorkoutId)
                .triggeringSnapshotId(triggeringSnapshotId)
                .previousVdot(previousVdot)
                .newVdot(newVdot)
                .flagged(shouldFlag)
                .accepted(!shouldFlag)
                .build();

        return repository.save(entry);
    }

    @Transactional(readOnly = true)
    public Optional<Double> getEffectiveVdot(User user) {
        return repository.findFirstByUserAndAcceptedTrueOrderByCalculatedAtDesc(user)
                .map(VdotHistory::getNewVdot);
    }

    @Transactional(readOnly = true)
    public List<VdotHistory> getHistory(User user) {
        return repository.findAllByUserOrderByCalculatedAtDesc(user);
    }

    @Transactional
    public void acceptFlagged(User user, UUID historyId) {
        VdotHistory entry = findOwnedEntry(user, historyId);
        if (!entry.isFlagged()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry is not flagged");
        }
        entry.setAccepted(true);
        repository.save(entry);
    }

    @Transactional
    public void dismissFlagged(User user, UUID historyId) {
        VdotHistory entry = findOwnedEntry(user, historyId);
        if (!entry.isFlagged()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Entry is not flagged");
        }
        // No mutation — entry stays flagged and unaccepted
    }

    private VdotHistory findOwnedEntry(User user, UUID historyId) {
        VdotHistory entry = repository.findById(historyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "VDOT history entry not found"));
        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "VDOT history entry not found");
        }
        return entry;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotHistoryServiceTest test`
Expected: All 13 tests PASS

- [ ] **Step 5: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryService.java \
       run-planner-backend/src/test/java/com/runplanner/vdot/VdotHistoryServiceTest.java
git commit -m "feat(vdot): implement VdotHistoryService with tests"
```

---

### Task 9: VdotHistoryResponse DTO and VdotController

**Files:**
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryResponse.java`
- Create: `run-planner-backend/src/test/java/com/runplanner/vdot/VdotControllerTest.java`
- Create: `run-planner-backend/src/main/java/com/runplanner/vdot/VdotController.java`

- [ ] **Step 1: Create VdotHistoryResponse DTO**

```java
package com.runplanner.vdot;

import java.time.Instant;
import java.util.UUID;

public record VdotHistoryResponse(
        UUID id,
        UUID triggeringWorkoutId,
        UUID triggeringSnapshotId,
        double previousVdot,
        double newVdot,
        Instant calculatedAt,
        boolean flagged,
        boolean accepted
) {
    public static VdotHistoryResponse from(VdotHistory entry) {
        return new VdotHistoryResponse(
                entry.getId(),
                entry.getTriggeringWorkoutId(),
                entry.getTriggeringSnapshotId(),
                entry.getPreviousVdot(),
                entry.getNewVdot(),
                entry.getCalculatedAt(),
                entry.isFlagged(),
                entry.isAccepted()
        );
    }
}
```

- [ ] **Step 2: Write failing controller tests**

Follow `GoalRaceControllerTest.java` patterns: `@WebMvcTest`, `@Import(SecurityConfig.class)`, `@MockBean`, MockMvc with `SecurityMockMvcRequestPostProcessors.user(user)`.

```java
package com.runplanner.vdot;

import com.runplanner.config.SecurityConfig;
import com.runplanner.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VdotController.class)
@Import(SecurityConfig.class)
class VdotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean VdotHistoryService vdotHistoryService;
    @MockBean com.runplanner.user.UserRepository userRepository;
    @MockBean com.runplanner.auth.JwtService jwtService;

    private User testUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("test@test.com")
                .passwordHash("hashed")
                .build();
    }

    // --- GET /history ---

    @Test
    void getHistory_authenticated_returnsHistoryList() throws Exception {
        User user = testUser();
        UUID workoutId = UUID.randomUUID();
        VdotHistory entry = VdotHistory.builder()
                .id(UUID.randomUUID())
                .user(user)
                .triggeringWorkoutId(workoutId)
                .previousVdot(50.0)
                .newVdot(52.0)
                .calculatedAt(Instant.now())
                .flagged(false)
                .accepted(true)
                .build();
        when(vdotHistoryService.getHistory(any())).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/v1/vdot/history").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousVdot").value(50.0))
                .andExpect(jsonPath("$[0].newVdot").value(52.0))
                .andExpect(jsonPath("$[0].flagged").value(false))
                .andExpect(jsonPath("$[0].accepted").value(true));
    }

    @Test
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vdot/history"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /history/{id}/accept ---

    @Test
    void acceptFlagged_authenticated_returns200() throws Exception {
        User user = testUser();
        UUID historyId = UUID.randomUUID();
        doNothing().when(vdotHistoryService).acceptFlagged(any(), eq(historyId));

        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/accept")
                        .with(user(user)))
                .andExpect(status().isOk());

        verify(vdotHistoryService).acceptFlagged(any(), eq(historyId));
    }

    @Test
    void acceptFlagged_unauthenticated_returns401() throws Exception {
        UUID historyId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/accept"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /history/{id}/dismiss ---

    @Test
    void dismissFlagged_authenticated_returns200() throws Exception {
        User user = testUser();
        UUID historyId = UUID.randomUUID();
        doNothing().when(vdotHistoryService).dismissFlagged(any(), eq(historyId));

        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/dismiss")
                        .with(user(user)))
                .andExpect(status().isOk());

        verify(vdotHistoryService).dismissFlagged(any(), eq(historyId));
    }

    @Test
    void dismissFlagged_unauthenticated_returns401() throws Exception {
        UUID historyId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/vdot/history/" + historyId + "/dismiss"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotControllerTest test`
Expected: Compilation failure — `VdotController` class not found

- [ ] **Step 4: Implement VdotController**

```java
package com.runplanner.vdot;

import com.runplanner.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vdot")
@RequiredArgsConstructor
public class VdotController {

    private final VdotHistoryService vdotHistoryService;

    @GetMapping("/history")
    public List<VdotHistoryResponse> getHistory(@AuthenticationPrincipal User user) {
        return vdotHistoryService.getHistory(user).stream()
                .map(VdotHistoryResponse::from)
                .toList();
    }

    @PostMapping("/history/{id}/accept")
    public void acceptFlagged(@AuthenticationPrincipal User user,
                              @PathVariable UUID id) {
        vdotHistoryService.acceptFlagged(user, id);
    }

    @PostMapping("/history/{id}/dismiss")
    public void dismissFlagged(@AuthenticationPrincipal User user,
                               @PathVariable UUID id) {
        vdotHistoryService.dismissFlagged(user, id);
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -f run-planner-backend/pom.xml -Dtest=VdotControllerTest test`
Expected: All 6 tests PASS

- [ ] **Step 6: Run the full test suite**

Run: `mvn -f run-planner-backend/pom.xml test`
Expected: All tests PASS (existing + new)

- [ ] **Step 7: Commit**

```bash
git add run-planner-backend/src/main/java/com/runplanner/vdot/VdotHistoryResponse.java \
       run-planner-backend/src/main/java/com/runplanner/vdot/VdotController.java \
       run-planner-backend/src/test/java/com/runplanner/vdot/VdotControllerTest.java
git commit -m "feat(vdot): add VdotController with history and accept/dismiss endpoints"
```
