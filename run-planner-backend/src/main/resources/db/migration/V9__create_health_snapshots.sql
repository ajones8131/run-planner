CREATE TABLE health_snapshots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vo2max_estimate DOUBLE PRECISION,
    resting_hr      INTEGER,
    recorded_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_health_snapshots_user_id ON health_snapshots(user_id);
CREATE INDEX idx_health_snapshots_user_recorded_at ON health_snapshots(user_id, recorded_at);

ALTER TABLE vdot_history
    ADD CONSTRAINT fk_vdot_triggering_snapshot
    FOREIGN KEY (triggering_snapshot_id) REFERENCES health_snapshots(id);
