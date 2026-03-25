CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255),
    date_of_birth   DATE,
    max_hr          INTEGER,
    preferred_units VARCHAR(10)  NOT NULL DEFAULT 'METRIC',
    last_synced_at  TIMESTAMPTZ
);
