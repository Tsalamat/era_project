alter table courses add column subject varchar(120) not null default 'Общее';
alter table courses add column cover_url varchar(500);

alter table lessons add column duration_minutes int not null default 15;

alter table blog_posts add column category varchar(120) not null default 'Подготовка';
alter table blog_posts add column read_minutes int not null default 1;

create table lesson_progress (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    lesson_id uuid not null references lessons(id) on delete cascade,
    completed_at timestamptz not null default now(),
    constraint lesson_progress_user_lesson_unique unique (user_id, lesson_id)
);

create index lesson_progress_user_idx on lesson_progress(user_id);
create index lessons_module_idx on lessons(module_id);
create index course_modules_course_idx on course_modules(course_id);
