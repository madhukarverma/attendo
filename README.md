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
| `:feature:*` | One module per screen/feature |

## Data files

Stored in the app's private storage (`files/data/`) and exportable from Settings:

| File | Columns |
|---|---|
| `subjects.csv` | `id,name,code,target_percent,created_at` |
| `attendance.csv` | `subject_id,date,status,updated_at` (`status` = PRESENT / ABSENT / CANCELLED) |
| `settings.csv` | `key,value` |

## Build

Requires JDK 17 and the Android SDK (platform 34).

```bash
./gradlew test assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.
