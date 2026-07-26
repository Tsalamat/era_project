create table roles (
    id bigserial primary key,
    name varchar(32) not null unique
);

create table users (
    id uuid primary key,
    full_name varchar(180) not null,
    email varchar(180) not null unique,
    password_hash varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table user_roles (
    user_id uuid not null references users(id) on delete cascade,
    role_id bigint not null references roles(id) on delete cascade,
    primary key (user_id, role_id)
);

create table courses (
    id uuid primary key,
    title varchar(220) not null,
    slug varchar(240) not null unique,
    description text,
    status varchar(32) not null default 'PUBLISHED',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table course_modules (
    id uuid primary key,
    course_id uuid not null references courses(id) on delete cascade,
    title varchar(220) not null,
    order_number int not null
);

create table lessons (
    id uuid primary key,
    module_id uuid not null references course_modules(id) on delete cascade,
    title varchar(220) not null,
    video_url varchar(500),
    content text,
    order_number int not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table lesson_files (
    id uuid primary key,
    lesson_id uuid not null references lessons(id) on delete cascade,
    file_name varchar(255) not null,
    file_url varchar(500) not null,
    content_type varchar(120)
);

create table tests (
    id uuid primary key,
    title varchar(220) not null,
    slug varchar(240) not null unique,
    description text,
    subject varchar(120) not null,
    time_limit_minutes int not null,
    status varchar(32) not null check (status in ('DRAFT', 'PUBLISHED')),
    created_by uuid references users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table questions (
    id uuid primary key,
    test_id uuid not null references tests(id) on delete cascade,
    question_text text not null,
    question_type varchar(32) not null check (question_type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE')),
    explanation text,
    order_number int not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table answer_options (
    id uuid primary key,
    question_id uuid not null references questions(id) on delete cascade,
    option_text text not null,
    is_correct boolean not null default false,
    order_number int not null
);

create table test_attempts (
    id uuid primary key,
    test_id uuid not null references tests(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    score int,
    max_score int,
    started_at timestamptz not null default now(),
    completed_at timestamptz
);

create table test_attempt_answers (
    id uuid primary key,
    attempt_id uuid not null references test_attempts(id) on delete cascade,
    question_id uuid not null references questions(id) on delete cascade,
    answer_option_id uuid not null references answer_options(id) on delete cascade
);

create table blog_posts (
    id uuid primary key,
    title varchar(220) not null,
    slug varchar(240) not null unique,
    excerpt text,
    content text not null,
    status varchar(32) not null default 'DRAFT',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table seo_metadata (
    id uuid primary key,
    path varchar(300) not null unique,
    title varchar(255) not null,
    description varchar(500) not null,
    keywords varchar(500),
    canonical varchar(500),
    og_title varchar(255),
    og_description varchar(500),
    og_image varchar(500),
    robots varchar(120) not null default 'index,follow'
);

create table reviews (
    id uuid primary key,
    author_name varchar(180) not null,
    content text not null,
    rating int not null,
    published boolean not null default false,
    created_at timestamptz not null default now()
);

create table faq_items (
    id uuid primary key,
    question varchar(500) not null,
    answer text not null,
    order_number int not null,
    published boolean not null default false
);

insert into roles(name) values ('STUDENT'), ('TEACHER'), ('ADMIN');
