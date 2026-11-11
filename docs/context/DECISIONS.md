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

## DEC-012 — Account backup in the Drive app-data folder via AuthorizationClient
- Date: 2026-11-09 · Status: Active
- Decision: Google sign-in = Play services `AuthorizationClient` requesting only `drive.appdata`
  (account picker + consent in one step); the user's email comes from Drive `about`. Drive is
  called over plain HTTPS (HttpURLConnection + org.json) instead of the Google API client
  libraries. One zip of all CSVs (+ launch image) per account, newest copy only; uploaded after
  changes (4 s debounce, skipped when the content hash is unchanged).
- Rationale: smallest possible permission (a private folder, not the user's files), no client
  secret or ID in the APK, ~no extra APK size, CSV stays the source of truth.
- Consequence: works only after an Android OAuth client (package + SHA-1) exists in Google Cloud
  (docs/GOOGLE_BACKUP_SETUP.md); a different signing key needs its SHA-1 added (ISSUE-006).

## DEC-013 — CSV restore matches files by header, replaces per table
- Date: 2026-11-09 · Status: Active
- Decision: each picked CSV is matched to a table by its required columns (not its file name) and
  replaces that table only, after a confirmation that lists row counts. Rows that don't decode are
  dropped. A Google restore is a full replace (tables missing from the backup are emptied).

## DEC-014 — Themes carry fonts; pale accents get separate fill colours
- Date: 2026-11-11 · Status: Active
- Decision: `AttendoPalette` holds `AttendoFonts` (system fonts only: sans, cursive, monospace,
  serif-monospace) and optional `accentFill`/`pinkFill`/`blueFill`; `AttendoColors.on(fill)` picks
  readable text on a fill. Settings › Theme is a dropdown of presets + Custom.
- Rationale: themes from the user's references (Ledger, Neon, Pastel, VS Code) differ in type as
  well as colour; Pastel's accents are too pale for text. No font files are bundled (size, licences).

## DEC-015 — Weekday checklist replaces the plain to-do list
- Date: 2026-11-11 · Status: Active
- Decision: Tasks = Checklist (habits on weekdays) · Board (Kanban) · Reminders (to-dos that have a
  time). Plain to-dos are migrated to board cards, idempotently, at start-up and after restores.
- Consequence: todos.csv now only holds reminders; nothing the user entered is dropped.

## DEC-016 — Security baseline
- Date: 2026-11-11 · Status: Active
- Decision: see docs/SECURITY.md (CSV-injection neutralising on export, bounded archive and network
  reads, HTTPS-only config, private lock-screen notifications, atomic CSV writes with recovery).
- Not done: data-at-rest encryption (CSV files must stay readable on export), release keystore.
