create table registration_verification_codes (
    id uuid primary key,
    email varchar(180) not null unique,
    full_name varchar(180) not null,
    password_hash varchar(255) not null,
    code_hash varchar(128) not null,
    attempts int not null default 0,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create index registration_verification_codes_expires_idx on registration_verification_codes(expires_at);
