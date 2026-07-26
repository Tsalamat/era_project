alter table blog_posts add column if not exists author_id uuid references users(id);

create table blog_comments (
    id uuid primary key,
    post_id uuid not null references blog_posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    content text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index blog_comments_post_idx on blog_comments(post_id);

create table blog_likes (
    id uuid primary key,
    post_id uuid not null references blog_posts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null default now(),
    constraint blog_likes_post_user_unique unique (post_id, user_id)
);

create index blog_likes_post_idx on blog_likes(post_id);

create table notifications (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    type varchar(64) not null,
    title varchar(220) not null,
    message text,
    link varchar(500),
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index notifications_user_created_idx on notifications(user_id, created_at desc);

create table test_assignments (
    id uuid primary key,
    test_id uuid not null references tests(id) on delete cascade,
    student_id uuid not null references users(id) on delete cascade,
    assigned_by uuid references users(id),
    assigned_at timestamptz not null default now(),
    completed_at timestamptz,
    constraint test_assignments_test_student_unique unique (test_id, student_id)
);

create index test_assignments_student_idx on test_assignments(student_id, assigned_at desc);
create index test_assignments_test_idx on test_assignments(test_id);
