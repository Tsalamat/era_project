create table proctoring_events (
    id uuid primary key,
    session_id uuid not null references proctoring_sessions(id) on delete cascade,
    event_type varchar(48) not null,
    details varchar(500),
    occurred_at timestamptz not null default now()
);

create index proctoring_events_session_idx on proctoring_events(session_id, occurred_at);
