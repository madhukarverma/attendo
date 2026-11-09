# Project: Attendo (Attendance Tracker) — Master Context

Follows the layering of `AI_CONTEXT_FRAMEWORK.md`: this file = durable project memory;
`CURRENT_STATE.md` = working memory; `DECISIONS.md` and `CHANGELOG.md` = decisions and episodic memory.

## Goal
Android app to track class attendance for every subject, styled after the "habit tracker" reference
designs (dark onboarding, cream home, green geometric mascots, handwritten speech bubbles).

## Scope v0.1 (bare bones)
- Onboarding (name).
- Subjects: add / list / delete, each with a target percentage (default 75%).
- Daily marking: Present / Absent / Cancelled per subject per day (one class per day per subject).
- Subject detail: percentage, classes you can skip / must attend, history.
- Stats: overall and per-subject.
- Settings: change name, export CSV files.

## Technology
- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose.
- AGP 8.5.2, Gradle 8.9, JDK 17, compileSdk/targetSdk 34, minSdk 26.
- Multi-module: `:app`, `:core:model`, `:core:data`, `:core:designsystem`, `:feature:*`.

## Constraints
- **All app data stored as CSV** in `filesDir/data/` (`settings.csv`, `subjects.csv`, `attendance.csv`).
- Feature modules must not depend on each other; they talk to `:core:data` interfaces only.
- Keep features minimal; refinement comes later.

## Design tokens
Ink `#232323`, Green `#50A65C`, Cream `#F8FAED` (from the reference), accents Pink `#E0399A`,
Blue `#2B8FE0`, Yellow `#E2D646`. Reference font Founders Grotesk is commercial, so system sans is used.

## Repository
GitHub `madhukarverma/attendo` (private). `master` = always-buildable trunk; one branch per feature.
