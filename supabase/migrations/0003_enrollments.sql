-- StudEdu: enrollments (ученик × предмет) и связки леджера (миграция 0003)
-- Выполнять в Supabase: SQL Editor → New query → вставить → Run

create table enrollments (
    id               uuid primary key default gen_random_uuid(),
    user_id          uuid not null references auth.users (id) on delete cascade,
    student_id       uuid not null references students (id) on delete cascade,
    subject          text not null,
    price_per_lesson numeric(10, 2),
    billing_mode     text not null default 'per_lesson'
        check (billing_mode in ('per_lesson', 'package', 'monthly')),
    monthly_fee      numeric(10, 2),
    active           boolean not null default true,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

create index idx_enrollments_student on enrollments (user_id, student_id);

alter table events         add column enrollment_id uuid references enrollments (id) on delete set null;
alter table lesson_records add column enrollment_id uuid references enrollments (id) on delete set null;
alter table payments       add column enrollment_id uuid references enrollments (id) on delete set null;
alter table payments       add column lesson_record_id uuid references lesson_records (id) on delete set null;

-- Предметы и ставки переехали в enrollments.
alter table students drop column if exists subject;
alter table students drop column if exists price_per_lesson;
alter table students drop column if exists monthly_fee;

create trigger trg_enrollments_updated before update on enrollments
    for each row execute function set_updated_at();

alter table enrollments enable row level security;

create policy "own rows select" on enrollments
    for select using (auth.uid() = user_id);
create policy "own rows insert" on enrollments
    for insert with check (auth.uid() = user_id);
create policy "own rows update" on enrollments
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own rows delete" on enrollments
    for delete using (auth.uid() = user_id);
