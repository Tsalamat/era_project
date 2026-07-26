create table proctoring_sessions (
    id uuid primary key,
    attempt_id uuid not null unique references test_attempts(id) on delete cascade,
    consent_at timestamptz not null,
    started_at timestamptz not null default now(),
    uploaded_at timestamptz,
    status varchar(32) not null,
    recording_path varchar(500),
    mime_type varchar(120),
    file_size bigint
);

create index proctoring_sessions_attempt_idx on proctoring_sessions(attempt_id);
