CREATE TABLE goal_races (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    distance_meters      INTEGER     NOT NULL,
    distance_label       VARCHAR(50) NOT NULL,
    race_date            DATE        NOT NULL,
    goal_finish_seconds  INTEGER,
    status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goal_races_user_id ON goal_races(user_id);
