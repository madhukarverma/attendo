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

## DEC-017 — Friends on Firebase (Auth + Firestore), one project with the Drive backup
- Date: 2026-11-12 · Status: Active
- Decision: friends need a shared backend; Firebase's free Spark plan covers a class. Google
  sign-in via Credential Manager (`GetSignInWithGoogleOption`) → Firebase Auth. Data model and
  privacy live in `firebase/firestore.rules` (profiles public to signed-in users; friends' lists
  and shared attendance/grades only to friends; friendships only via an accepted request).
  Firebase is initialised in code from BuildConfig (no Google Services plugin), so the app builds
  and runs without a project.
- Consequence: the Drive backup must use the same project (one Android OAuth client per package +
  SHA-1). Profiles expose name, programme and friend code to any signed-in student.

## DEC-018 — Storage problems never crash the app or lose data
- Date: 2026-11-15 · Status: Active
- Decision: `CsvStore` treats storage errors as expected. A file that can't be read is copied to
  `<name>.unreadable-<time>` before the table starts empty (if even the copy fails, saving to that
  table is refused until restart); a failed save keeps the previous data. Both are reported on
  `StorageProblems` and shown as a toast by the app.
- Consequence: repositories don't need their own error handling for disk problems, and the user
  hears about them instead of losing a change silently.

## DEC-019 — Target Android 15 (SDK 35) on AGP 8.5
- Date: 2026-11-15 · Status: Active
- Decision: compileSdk/targetSdk 35 with the existing AGP 8.5.2 (`android.suppressUnsupportedCompileSdk=35`
  in `gradle.properties`) rather than a major AGP upgrade in the same release. The app was already
  edge-to-edge, so Android 15's enforced edge-to-edge changes nothing.
- Consequence: an AGP and library update remains a separate task.

## DEC-020 — Release key outside the repository
- Date: 2026-11-15 · Status: Active
- Decision: a PKCS12 key (`attendo`, RSA 4096) in `%USERPROFILE%\android-dev\keys`, read through a
  git-ignored `keystore.properties`. The user chose the password; it never appears in the repo or
  in chat. Without the file, release builds fall back to the debug key with a warning, so the
  project still builds anywhere.
- Consequence: phones on v0.32.0 or older must uninstall once (after a backup) to move to the
  release-signed app. The key's SHA-1 must be added in Firebase for Google sign-in.

## DEC-021 — Apple's fluid-interface rules as the motion and material standard
- Date: 2026-11-15 · Status: Active
- Decision: follow the user's "apple-design" skill (`.claude/skills/apple-design/SKILL.md`; WWDC18 "Designing Fluid Interfaces" and
  related talks), translated to Compose: springs critically damped by default (`snappy`,
  `settle`), bounce only after momentum (`release`, damping 0.8 / response 0.3); gestures start
  from the on-screen value, hand their release velocity to the spring, and decide flicks with
  Apple's projection (`Physics.project`); Apple's rubber-band formula; floating chrome as frosted
  glass; size-specific tracking; haptics only for meaningful moments via `rememberHaptics()`.
  The glass blur is built on Compose 1.7 graphics layers instead of adding a blur library.
- Consequence: blur needs Android 12+ (older phones get a near-solid tab bar). Feature code
  should use these helpers rather than ad-hoc tweens or raw haptic calls.

## DEC-022 — Animations stay in the draw phase
- Date: 2026-11-15 · Status: Active
- Decision: per `.claude/skills/animation-performance/SKILL.md`, a value that changes every frame
  is read only inside draw or layout lambdas (`graphicsLayer {}`, `drawBehind {}`, `Canvas`, or a
  `() -> Float` passed down), and progress is drawn, not laid out (`ProgressBar`).
- Consequence: moving things cost a redraw of their own layer, not a recomposition or layout of
  the screen. Short colour cross-fades on small controls are allowed to stay as they are.

## DEC-023 — Rules are tested before they're published; libraries stay buildable
- Date: 2026-11-15 · Status: Active
- Decision: every change to `firebase/firestore.rules` passes `firebase/test/` on the local
  emulator first, with a test for each app write and each attack. Only Google accounts count as
  signed in, and timestamps must equal `request.time`. Libraries are updated to the newest versions
  the current toolchain compiles; the toolchain upgrade itself (AGP 9, Kotlin 2.4) is separate
  work that needs a phone to test on (ISSUE-023).
- Consequence: the test suite adds Node tooling under `firebase/test/` (not part of the Android
  build). No server-side rate limiting: the free Firebase plan can't do it (`docs/SECURITY.md`).

## DEC-024 — One spacing scale, one radius, one font, icons, coded errors
- Date: 2026-11-15 · Status: Partly replaced by DEC-026 (font and capsules); spacing, icons, empty
  states and error codes still apply
- Decision: paddings and gaps use only `Space` (4, 8, 12, 16, 24, 32, 48 dp; old values were
  rounded to the nearest step, ties up). Every rounded shape is `Corner.Shape` (16 dp), buttons
  and chips included; `CircleShape` is kept only for circles (dots, avatars, round buttons, the
  dial). The app font is Plus Jakarta Sans, bundled as one variable font (Ledger and VS Code
  themes keep their monospace faces). Marks use icons, never text symbols or emoji. Empty lists
  use `EmptyState`. Every error the user sees comes from `ErrorCode` and ends with its code.
- Consequence: the look changed (rounded rectangles instead of pills, a little more space) without
  being checked on a phone; review screenshots before the next visual change. Chart marks keep
  their own small radii (data, not chrome).

## DEC-025 — Feedback goes to Firestore, write-only, no sign-in
- Date: 2026-11-15 · Status: Active
- Decision: feedback is a document in the existing Firestore project, sent without sign-in so
  anyone can report a problem. The rules allow create only, with fixed fields and limits; the
  developer reads it in the Firebase console. No email or ticket service, and no user id stored.
- Consequence: no new account or cost. The trade-off is that unauthenticated writes could be
  scripted (docs/FEEDBACK.md); requiring sign-in is a one-line rules change if needed.

## DEC-026 — Keep the Apple look: system font, capsule controls, font is a setting
- Date: 2026-11-15 · Status: Active
- Decision: the app stays on the apple-design skill. The default font is the phone's own
  ("default to the platform's system font"), and other fonts are a choice in Settings › Font:
  the five most used Google Fonts (Roboto, Open Sans, Montserrat, Lato, Poppins), Plus Jakarta Sans,
  and the fonts the themes use. Buttons, chips, the tab bar and the day strip are capsules again;
  cards, fields and sheets keep the 16 dp radius. Bundled fonts ship as separate files per weight,
  because some phones ignored the variable font's weight (bold titles showed as regular).
- Consequence: about 2 MB of fonts in the APK (Lato's licence forbids trimming it; the others are
  cut to Latin text, other scripts fall back to the phone's font).
