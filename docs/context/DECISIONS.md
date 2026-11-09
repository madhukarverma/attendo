# Decisions

## DEC-001 — CSV files as the storage format
- Date: 2026-11-09 · Status: Active
- Decision: Persist everything in CSV (`settings.csv`, `subjects.csv`, `attendance.csv`).
- Rationale: Explicit requirement — data must be easy to open/export later (Excel, Sheets, scripts).
- Details: header-based column mapping (new columns don't break old files); atomic temp-file writes;
  in-memory `StateFlow` cache with a mutex per file.
- Consequences: fine for hundreds/thousands of rows; revisit (Room + CSV export) if data grows large.

## DEC-002 — Multi-module architecture
- Date: 2026-11-09 · Status: Active
- Decision: `core:model` and `core:data` are pure Kotlin/JVM (fast unit tests), `core:designsystem`
  holds Compose UI kit, each feature is its own Android library.
- Rationale: modularity requirement; parallel builds; features can be added/removed independently.

## DEC-003 — Manual DI instead of Hilt
- Date: 2026-11-09 · Status: Active
- Decision: `AppContainer` in `:app` creates repositories; routes receive interfaces.
- Rationale: bare-bones v0.1, avoids kapt/KSP setup. Hilt can be introduced later without touching screens.

## DEC-004 — One attendance mark per subject per day
- Date: 2026-11-09 · Status: Active (revisit)
- Decision: `attendance.csv` keyed by (subject_id, date).
- Consequence: double lectures are not yet representable — see OPEN_ISSUES.

## DEC-006 — iOS-style motion as a switchable design-system layer
- Date: 2026-11-09 · Status: Active
- Decision: motion lives in `core/designsystem/motion` (`IosMotion` tokens, `BounceContainer`,
  `iosPress`, `CollapsingTopBar`) and in `app/navigation/Transitions.kt`; `LocalIosMotion` turns it off.
- Rationale: one place to tune curves; features opt in through shared components; users can disable it.
- Notes: swipe-back relies on Android predictive back (Android 14+ with gesture navigation); older
  versions get the same animation on the back button. System "Remove animations" is respected.

## DEC-007 — Global animation kill-switch via MotionDurationScale
- Date: 2026-11-09 · Status: Active (supersedes the Material-fallback part of DEC-006)
- Decision: MainActivity hosts Compose on its own Recomposer whose context carries
  `AppMotionScale`; the Settings "Animations" switch sets its scale to 0 so all Compose animations
  (including Material components) complete instantly. Flings keep their own scale.
- Consequence: the activity manages the Recomposer lifecycle (frame clock paused in onStop,
  cancelled in onDestroy). Not yet verified on a physical device.

## DEC-005 — Release APK signed with debug key
- Date: 2026-11-09 · Status: Active (testing only)
- Rationale: no release keystore yet; lets the user install and test. Replace before any store upload.

## DEC-008 — Bottom tab navigation with a Home summary
- Date: 2026-11-09 · Status: Active
- Decision: tabs Home · Classes · To-do · Timer · Stats in one `AttendoTabBar` drawn above the
  NavHost (hidden on pushed screens). Tab switches use `popUpTo(HOME){saveState}` +
  `restoreState` + `launchSingleTop`. Settings lives behind the gear on Home.
- Rationale: the user asked for a Home button and a summary page; five sections no longer fit as
  pushed screens from one main page. Five tabs fit a 360 dp wide phone; a sixth (Settings) didn't.
- Consequence: `:feature:home` was renamed `:feature:attendance`; the summary is `:feature:dashboard`.

## DEC-009 — Reminders via AlarmManager mirrored from todos.csv
- Date: 2026-11-09 · Status: Active
- Decision: the app module's `ReminderScheduler` re-syncs one exact alarm per pending reminder every
  time todos.csv changes, at start-up, and after boot / app update / clock change. The receiver
  re-reads the file before notifying. Exact alarms use `USE_EXACT_ALARM` (API 33+) and
  `SCHEDULE_EXACT_ALARM` (API 31-32), falling back to inexact alarms if not allowed.
- Rationale: no WorkManager dependency; the CSV stays the single source of truth.
- Note: `USE_EXACT_ALARM` is restricted on Google Play to alarm/calendar apps; revisit before any
  store upload (switch to SCHEDULE_EXACT_ALARM + a settings prompt).

## DEC-010 — Lecture hours: one hour = one lecture = one mark
- Date: 2026-11-09 · Status: Active
- Decision: `Subject.lectureHours` (optional) is the course's total lectures; lectures left = hours -
  held (present + absent). Cancelled classes don't count. Course outlook uses the same target maths.
- Consequence: depends on DEC-004 (one mark per day); a double lecture still counts once (ISSUE-001).

## DEC-011 — Nickname is derived, not stored
- Date: 2026-11-09 · Status: Active
- Decision: `Nickname.of(name)`: names longer than 10 characters show as their first 4 letters.
  Only the full name is stored, so the rule can change without migrating settings.csv.
