-- StudEdu: правила скрытия пар вуза (миграция 0002)
-- Выполнять в Supabase: SQL Editor → New query → вставить → Run

create table hidden_lesson_rules (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references auth.users (id) on delete cascade,
    provider    text not null,               -- 'mospolytech'
    "group"     text not null,
    subject     text not null,
    lesson_type text,                        -- null = любой тип занятия
    mode        text not null default 'hide' check (mode in ('hide', 'dim')),
    created_at  timestamptz not null default now()
);

create index idx_hidden_rules_user on hidden_lesson_rules (user_id, provider, "group");

alter table hidden_lesson_rules enable row level security;

create policy "own rows select" on hidden_lesson_rules
    for select using (auth.uid() = user_id);
create policy "own rows insert" on hidden_lesson_rules
    for insert with check (auth.uid() = user_id);
create policy "own rows update" on hidden_lesson_rules
    for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own rows delete" on hidden_lesson_rules
    for delete using (auth.uid() = user_id);
