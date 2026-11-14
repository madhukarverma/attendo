# Attendo

A small, modular Android app (Kotlin + Jetpack Compose) for tracking class attendance per subject.
All data is stored as plain **CSV** files, so it can be opened in Excel/Sheets at any time.

- Flowcharts (delivery plan, navigation, architecture): [docs/FLOWCHART.md](docs/FLOWCHART.md)
- Project context, decisions, open issues: [docs/context/](docs/context/)

## Modules

| Module | Purpose |
|---|---|
| `:app` | Application, DI container, navigation |
| `:core:model` | Pure Kotlin domain models + attendance maths |
| `:core:data` | Repository interfaces + CSV implementation |
| `:core:designsystem` | Theme, colours, mascots, shared Compose components |
| `:core:backup` | Google account backup (Drive app-data folder) |
| `:core:social` | Friends on Firebase (sign-in, requests, shared attendance/grades) |
| `:feature:*` | One module per screen/feature |

## Data files

Stored in the app's private storage (`files/data/`) and exportable from Settings:

| File | Columns |
|---|---|
| `settings.csv` | `key,value` |
| `subjects.csv` | `id,name,code,target_percent,created_at,lecture_hours` |
| `attendance.csv` | `subject_id,date,status,updated_at,count` (`status` = PRESENT / ABSENT / CANCELLED) |
| `grades.csv` | `id,subject_id,title,scored,max_marks,date,created_at` |
| `timetable.csv` | `id,subject_id,day,start,end,room` |
| `events.csv` | `id,title,start,end,kind,note,created_at` (your own calendar entries) |
| `syllabus.csv` | `code,unit,covered_at` |
| `pomodoro.csv` | `id,date,started_at,kind,subject_id,planned_seconds,actual_seconds,completed` |
| `todos.csv` | `id,title,date,remind_at,done,created_at` (reminders) |
| `habits.csv` | `id,title,days,position,created_at` (weekday checklist) |
| `habit_log.csv` | `habit_id,date` |
| `kanban_columns.csv` | `id,title,position,done` |
| `kanban_cards.csv` | `id,column_id,title,notes,due,priority,position,created_at` |

A file that can't be read is kept as `<name>.unreadable-<time>` before anything is saved over it.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```bash
./gradlew test assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. Release builds are signed
with the key described in [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md).
