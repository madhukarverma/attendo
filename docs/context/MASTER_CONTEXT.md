# Project: Attendo (Attendance Tracker) — Master Context

Follows the layering of `AI_CONTEXT_FRAMEWORK.md`: this file = durable project memory;
`CURRENT_STATE.md` = working memory; `DECISIONS.md` and `CHANGELOG.md` = decisions and episodic memory.

## Goal
Android app to track class attendance for every subject, styled after the "habit tracker" reference
designs (dark onboarding, cream home, green geometric mascots, handwritten speech bubbles).

## Original scope (v0.1, bare bones — kept for history; today's features are in CURRENT_STATE.md)
- Onboarding (name).
- Subjects: add / list / delete, each with a target percentage (default 75%).
- Daily marking: Present / Absent / Cancelled per subject per day (one class per day per subject).
- Subject detail: percentage, classes you can skip / must attend, history.
- Stats: overall and per-subject.
- Settings: change name, export CSV files.

## Technology
- Kotlin 2.0.20, Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose.
- AGP 8.5.2, Gradle 8.9, JDK 17, compileSdk/targetSdk 35 (since v0.32), minSdk 26.
- Multi-module: `:app`, `:core:model`, `:core:data`, `:core:designsystem`, `:feature:*`.

## Constraints
- **All app data stored as CSV** in `filesDir/data/` (`settings.csv`, `subjects.csv`, `attendance.csv`,
  `pomodoro.csv`, `todos.csv`).
- Feature modules must not depend on each other; they talk to `:core:data` interfaces only
  (and `:core:backup` for the optional Google account backup).
- Works fully offline; the network is used only for the optional Google account backup.
- Keep features minimal; refinement comes later.

## Design tokens
Ink `#232323`, Green `#50A65C`, Cream `#F8FAED` (from the reference), accents Pink `#E0399A`,
Blue `#2B8FE0`, Yellow `#E2D646`. Font: the phone's own by default (Apple: system font first);
Settings › Font offers Roboto, Open Sans, Montserrat, Lato, Poppins, Plus Jakarta Sans and the theme
fonts. Spacing scale 4/8/12/16/24/32/48 dp, Apple continuous corners (each box its own radius),
capsule buttons, icons not
symbols, designed empty states, error messages with codes (DEC-024, DEC-026). Mascots follow a DNA
nucleotide: pentagon (sugar) on Home, circle (phosphate) on Classes, rectangle (base) on Tasks.
Motion and materials follow Apple's design talks (DEC-021): critically damped springs, velocity
handoff, flick projection, rubber banding, frosted glass for floating chrome, size-specific tracking.

## Repository
GitHub `madhukarverma/attendo` (private since 2026-11-15; it was public before, so treat earlier commits as already seen, and still never commit anything personal or secret). `master` = always-buildable trunk; one branch per feature.
