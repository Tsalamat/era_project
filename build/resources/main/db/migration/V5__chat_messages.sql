create table chat_messages (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    message text not null,
    created_at timestamptz not null default now()
);

create index chat_messages_created_at_idx on chat_messages(created_at);
create index chat_messages_user_idx on chat_messages(user_id);
