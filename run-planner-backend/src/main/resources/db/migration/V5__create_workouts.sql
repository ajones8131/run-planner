CREATE TABLE workouts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source              VARCHAR(50) NOT NULL,
    source_id           VARCHAR(255),
    started_at          TIMESTAMPTZ NOT NULL,
    distance_meters     DOUBLE PRECISION NOT NULL,
    duration_seconds    INTEGER NOT NULL,
    avg_hr              INTEGER,
    max_hr              INTEGER,
    elevation_gain      DOUBLE PRECISION
);

CREATE INDEX idx_workouts_user_id ON workouts(user_id);
CREATE INDEX idx_workouts_user_started_at ON workouts(user_id, started_at);
CREATE UNIQUE INDEX idx_workouts_source_source_id ON workouts(source, source_id);

ALTER TABLE vdot_history
    ADD CONSTRAINT fk_vdot_triggering_workout
    FOREIGN KEY (triggering_workout_id) REFERENCES workouts(id);
