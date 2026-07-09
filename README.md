# StudEdu

Личное приложение репетитора-студента: репетиторское расписание + пары вуза (Московский политех) + оплаты учеников, с подсветкой конфликтов по времени.

Стек: Kotlin, Jetpack Compose (Material 3), Room (offline-first), Supabase (Postgres + Auth + Realtime, RLS), kotlinx-datetime, dmfs/lib-recur.

## Статус

Сделано: **схема БД** (SQL-миграции + Room) и **Этап 0** (каркас, 4 вкладки).
Дальше по плану: Этап 1 — главный экран на ручных событиях.

## Как запустить

### 1. Открыть проект

Android Studio → **Open** → выбрать папку `studedu`.

В проекте нет `gradle-wrapper.jar` (бинарник, не хранится в репозитории) — Android Studio сам скачает Gradle 8.9 по `gradle-wrapper.properties` при первой синхронизации. Если синк споткнётся о wrapper: **Settings → Build Tools → Gradle → Distribution: Wrapper**, либо в терминале AS выполнить `gradle wrapper --gradle-version 8.9` (создаст jar). После успешного синка: **Run ▶** на эмуляторе или телефоне.

### 2. Supabase — уже настроен

Миграция `supabase/migrations/0001_initial_schema.sql` применена к проекту WhiseMoen's Project (8 таблиц + RLS). URL и publishable-ключ уже вписаны в `SupabaseClient.kt`. Ключ не секретный — данные защищает RLS.

Важно: на free-плане Supabase усыпляет проект после ~недели неактивности. Если синк перестал работать — зайди в панель и нажми Resume project.

### 3. GitHub

```
git init
git add .
git commit -m "Этап 0: каркас + схема БД"
git remote add origin <url>
git push -u origin main
```

## Структура

```
supabase/migrations/          SQL-миграции (Postgres + RLS)
app/src/main/java/com/sapraliev/studedu/
  MainActivity.kt
  ui/navigation/              нижняя навигация, 4 вкладки
  ui/screens/                 экраны (пока заглушки)
  ui/theme/                   Material 3 тема
  data/local/                 Room: сущности, DAO, база, конвертеры
  data/remote/                клиент Supabase
```

## Принципы (из спецификации)

- Offline-first: Room — источник правды для UI, Supabase — синхронизация.
- Повторяющиеся события: правило RRULE + исключения, без дублирования строк.
- Расписание вуза: read-only кэш, синк по кнопке / раз в день, абстракция `ScheduleProvider` (Этап 2).
- Неоморфизм — только точечные акценты; база — читаемый Material 3.

## План этапов

1. Главный экран на ручных событиях (лента, создание с повторением).
2. `ScheduleProvider` + `MospolytechProvider`, кэш пар, подсветка конфликтов.
3. Ученики, занятия, леджер оплат.
4. Заметки + дедлайны.
5. Настройки и полировка.
