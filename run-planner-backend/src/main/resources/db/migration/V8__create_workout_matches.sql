CREATE TABLE workout_matches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planned_workout_id  UUID NOT NULL REFERENCES planned_workouts(id) ON DELETE CASCADE,
    workout_id          UUID NOT NULL REFERENCES workouts(id) ON DELETE CASCADE,
    compliance_score    DOUBLE PRECISION NOT NULL
);

CREATE UNIQUE INDEX idx_workout_matches_planned ON workout_matches(planned_workout_id);
CREATE UNIQUE INDEX idx_workout_matches_workout ON workout_matches(workout_id);
