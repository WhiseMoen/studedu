-- ============================================================
-- StudEdu: начальная схема БД (раздел 4 спецификации)
-- Выполнять в Supabase: SQL Editor → New query → вставить → Run
-- ============================================================

-- ---------- ENUM-типы ----------
create type event_type as enum ('personal', 'lesson', 'deadline');
create type recurrence_freq as enum ('daily', 'weekly', 'monthly');
create type exception_type as enum ('cancelled', 'moved');
create type payment_direction as enum ('charge', 'payment');
create type task_source as enum ('personal', 'university_deadline');

-- ---------- 4.1. Повторения ----------
create table recurrence_rules (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users (id) on delete cascade,
    freq        recurrence_freq not null,
    interval    int not null default 1 check (interval >= 1),
    byweekday   text[],            -- например '{MO,WE,FR}'
    count       int,               -- сколько раз (взаимоисключимо с until)
    until       date,              -- до какой даты
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

-- ---------- 4.2. Ученики (до events: на них ссылается events.student_id) ----------
create table students (
    id               uuid primary key default gen_random_uuid(),
    user_id          uuid not null references auth.users (id) on delete cascade,
    name             text not null,
    contact          text,
    subject          text,
    price_per_lesson numeric(10, 2),
    monthly_fee      numeric(10, 2),
    active           boolean not null default true,
    notes            text,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

-- ---------- 4.1. События ----------
-- Пары вуза здесь НЕ хранятся (они в university_schedule_cache).
create table events (
    id                 uuid primary key default gen_random_uuid(),
    user_id            uuid not null references auth.users (id) on delete cascade,
    title              text not null,
    comment            text,
    type               event_type not null default 'personal',
    start_at           timestamptz not null,  -- для серии: первое вхождение (шаблон)
    end_at             timestamptz not null,
    is_all_day         boolean not null default false,
    student_id         uuid references students (id) on delete set null,
    recurrence_rule_id uuid references recurrence_rules (id) on delete set null,
    color              text,
    source             text,
    created_at         timestamptz not null default now(),
    updated_at         timestamptz not null default now(),
    check (end_at >= start_at)
);

create table recurrence_exceptions (
    id            uuid primary key default gen_random_uuid(),
    user_id       uuid not null references auth.users (id) on delete cascade,
    event_id      uuid not null references events (id) on delete cascade,
    original_date timestamptz not null,   -- какое вхождение серии
    type          exception_type not null,
    new_start_at  timestamptz,            -- если перенос
    new_end_at    timestamptz,
    created_at    timestamptz not null default now(),
    unique (event_id, original_date)
);

-- ---------- 4.2. Занятия и оплаты ----------
create table lesson_records (
    id             uuid primary key default gen_random_uuid(),
    user_id        uuid not null references auth.users (id) on delete cascade,
    student_id     uuid not null references students (id) on delete cascade,
    event_id       uuid references events (id) on delete set null,
    date           date not null,
    topics_covered text,
    homework       text,
    attended       boolean not null default true,
    created_at     timestamptz not null default now()
);

-- Леджер: баланс ученика = SUM(payment) - SUM(charge)
create table payments (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references auth.users (id) on delete cascade,
    student_id uuid not null references students (id) on delete cascade,
    amount     numeric(10, 2) not null check (amount > 0),
    direction  payment_direction not null,
    date       date not null default current_date,
    comment    text,
    created_at timestamptz not null default now()
);

-- ---------- 4.3. Задачи и кэш расписания ----------
create table tasks (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references auth.users (id) on delete cascade,
    text       text not null,
    done       boolean not null default false,
    tags       text[] not null default '{}',
    due_date   date,                -- если задано — это дедлайн
    source     task_source not null default 'personal',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- Read-only кэш пар вуза; перезаписывается при синке.
create table university_schedule_cache (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users (id) on delete cascade,
    provider    text not null,      -- 'mospolytech'
    "group"     text not null,
    subject     text not null,
    teacher     text,
    place       text,
    lesson_type text,
    start_at    timestamptz not null,
    end_at      timestamptz not null,
    synced_at   timestamptz not null default now()
);

-- ---------- Индексы ----------
create index idx_events_user_start on events (user_id, start_at);
create index idx_events_student on events (student_id);
create index idx_exceptions_event on recurrence_exceptions (event_id);
create index idx_lesson_records_student on lesson_records (user_id, student_id, date);
create index idx_payments_student on payments (user_id, student_id, date);
create index idx_tasks_user_due on tasks (user_id, due_date);
create index idx_cache_user_start on university_schedule_cache (user_id, start_at);

-- ---------- Автообновление updated_at ----------
create or replace function set_updated_at()
returns trigger language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end $$;

create trigger trg_events_updated before update on events
    for each row execute function set_updated_at();
create trigger trg_rules_updated before update on recurrence_rules
    for each row execute function set_updated_at();
create trigger trg_students_updated before update on students
    for each row execute function set_updated_at();
create trigger trg_tasks_updated before update on tasks
    for each row execute function set_updated_at();

-- ---------- RLS: каждый видит только свои строки ----------
alter table recurrence_rules          enable row level security;
alter table students                  enable row level security;
alter table events                    enable row level security;
alter table recurrence_exceptions     enable row level security;
alter table lesson_records            enable row level security;
alter table payments                  enable row level security;
alter table tasks                     enable row level security;
alter table university_schedule_cache enable row level security;

do $$
declare t text;
begin
    foreach t in array array[
        'recurrence_rules', 'students', 'events', 'recurrence_exceptions',
        'lesson_records', 'payments', 'tasks', 'university_schedule_cache'
    ] loop
        execute format(
            'create policy "own rows select" on %I for select using (auth.uid() = user_id)', t);
        execute format(
            'create policy "own rows insert" on %I for insert with check (auth.uid() = user_id)', t);
        execute format(
            'create policy "own rows update" on %I for update using (auth.uid() = user_id) with check (auth.uid() = user_id)', t);
        execute format(
            'create policy "own rows delete" on %I for delete using (auth.uid() = user_id)', t);
    end loop;
end $$;
