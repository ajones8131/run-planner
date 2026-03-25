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
