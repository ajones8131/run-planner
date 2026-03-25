package com.runplanner.plan;

import com.runplanner.vdot.PaceRange;
import com.runplanner.vdot.TrainingPaceCalculator;
import com.runplanner.vdot.TrainingZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.runplanner.plan.PlanConstants.*;

@Component
@RequiredArgsConstructor
public class TrainingPlanGenerator {

    private final TrainingPaceCalculator trainingPaceCalculator;

    // V1: raceDistanceMeters is accepted for API compatibility but not yet used in mileage
    // calculation. Currently weekly mileage is derived purely from VDOT. Future enhancement:
    // scale mileage based on target race distance (e.g., marathon plans need higher volume).
    public List<PlannedWorkout> generate(double vdot, int raceDistanceMeters,
                                          LocalDate raceDate, LocalDate startDate) {
        int totalWeeks = (int) ChronoUnit.WEEKS.between(startDate, raceDate);
        if (totalWeeks <= 0) {
            throw new IllegalArgumentException("Race date must be after start date by at least one week");
        }

        Map<TrainingZone, PaceRange> paces = trainingPaceCalculator.calculate(vdot);
        double weeklyTargetKm = MILEAGE_BASE_KM + (vdot - 30.0) * MILEAGE_PER_VDOT;
        int[] phaseLengths = calculatePhaseLengths(totalWeeks);

        List<PlannedWorkout> workouts = new ArrayList<>();
        int weekNumber = 1;

        for (int phaseIdx = 0; phaseIdx < phaseLengths.length; phaseIdx++) {
            TrainingPhase phase = TrainingPhase.values()[phaseIdx];
            int phaseWeeks = phaseLengths[phaseIdx];

            for (int weekInPhase = 0; weekInPhase < phaseWeeks; weekInPhase++) {
                double mileageFactor = calculateMileageFactor(phase, weekInPhase, phaseWeeks);
                double weekDistanceKm = weeklyTargetKm * mileageFactor;
                WorkoutType[] template = WEEKLY_TEMPLATES.get(phase);

                for (int day = 0; day < 7; day++) {
                    LocalDate date = startDate.plusWeeks(weekNumber - 1).plusDays(day);
                    WorkoutType type = template[day];
                    double distanceMeters = calculateDayDistance(type, template, weekDistanceKm) * 1000.0;

                    PaceRange pace = null;
                    if (type.getTrainingZone() != null) {
                        pace = paces.get(type.getTrainingZone());
                    }

                    workouts.add(PlannedWorkout.builder()
                            .weekNumber(weekNumber)
                            .dayOfWeek(date.getDayOfWeek().getValue())
                            .scheduledDate(date)
                            .workoutType(type)
                            .targetDistanceMeters(distanceMeters)
                            .targetPaceMinPerKm(pace != null ? pace.minPaceMinPerKm() : null)
                            .targetPaceMaxPerKm(pace != null ? pace.maxPaceMinPerKm() : null)
                            .originalScheduledDate(date)
                            .planRevision(1)
                            .build());
                }
                weekNumber++;
            }
        }

        return workouts;
    }

    int[] calculatePhaseLengths(int totalWeeks) {
        if (totalWeeks < MIN_WEEKS_FULL_PLAN) {
            int taper = Math.max(1, totalWeeks / 2);
            int peak = totalWeeks - taper;
            return new int[]{0, 0, peak, taper};
        }

        int base = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * BASE_PHASE_PCT));
        int quality = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * QUALITY_PHASE_PCT));
        int peak = Math.max(MIN_WEEKS_PER_PHASE, (int) Math.round(totalWeeks * PEAK_PHASE_PCT));
        int taper = totalWeeks - base - quality - peak;
        taper = Math.max(MIN_WEEKS_PER_PHASE, taper);

        int total = base + quality + peak + taper;
        if (total > totalWeeks) {
            taper -= (total - totalWeeks);
            if (taper < MIN_WEEKS_PER_PHASE) {
                throw new IllegalArgumentException(
                        "Plan too short for 4-phase periodization with " + totalWeeks + " weeks");
            }
        } else if (total < totalWeeks) {
            base += (totalWeeks - total);
        }

        return new int[]{base, quality, peak, taper};
    }

    private double calculateMileageFactor(TrainingPhase phase, int weekInPhase, int phaseWeeks) {
        double progress = phaseWeeks > 1 ? (double) weekInPhase / (phaseWeeks - 1) : 0.0;
        return switch (phase) {
            case BASE -> BASE_START_FACTOR + progress * (BASE_END_FACTOR - BASE_START_FACTOR);
            case QUALITY -> QUALITY_FACTOR;
            case PEAK -> PEAK_START_FACTOR + progress * (PEAK_END_FACTOR - PEAK_START_FACTOR);
            case TAPER -> TAPER_START_FACTOR + progress * (TAPER_END_FACTOR - TAPER_START_FACTOR);
        };
    }

    private double calculateDayDistance(WorkoutType type, WorkoutType[] template, double weekDistanceKm) {
        if (type == WorkoutType.REST) {
            return REST_DISTANCE;
        }

        int easyCount = 0;
        boolean hasLong = false;
        int qualityCount = 0;

        for (WorkoutType t : template) {
            switch (t) {
                case REST -> {}
                case LONG -> hasLong = true;
                case EASY -> easyCount++;
                default -> qualityCount++;
            }
        }

        double remaining = weekDistanceKm;
        double longDistance = hasLong ? weekDistanceKm * LONG_RUN_PCT : 0;
        remaining -= longDistance;

        double qualityDistance = qualityCount > 0 ? weekDistanceKm * QUALITY_SESSION_PCT : 0;
        double totalQuality = qualityDistance * qualityCount;
        remaining -= totalQuality;

        double easyDistance = easyCount > 0 ? remaining / easyCount : 0;

        return switch (type) {
            case LONG -> longDistance;
            case EASY -> easyDistance;
            case REST -> REST_DISTANCE;
            default -> qualityDistance;
        };
    }
}
