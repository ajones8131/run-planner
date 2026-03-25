CREATE TABLE planned_workouts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_plan_id        UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    week_number             INTEGER NOT NULL,
    day_of_week             INTEGER NOT NULL,
    scheduled_date          DATE NOT NULL,
    workout_type            VARCHAR(20) NOT NULL,
    target_distance_meters  DOUBLE PRECISION NOT NULL,
    target_pace_min_per_km  DOUBLE PRECISION,
    target_pace_max_per_km  DOUBLE PRECISION,
    target_hr_zone          INTEGER,
    notes                   TEXT,
    original_scheduled_date DATE NOT NULL,
    plan_revision           INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_planned_workouts_plan_id ON planned_workouts(training_plan_id);
CREATE INDEX idx_planned_workouts_scheduled_date ON planned_workouts(scheduled_date);
