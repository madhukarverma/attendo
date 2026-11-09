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

## DEC-005 — Release APK signed with debug key
- Date: 2026-11-09 · Status: Active (testing only)
- Rationale: no release keystore yet; lets the user install and test. Replace before any store upload.
