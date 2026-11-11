# Current State

## Update 2026-11-12
v0.28.0 grades, v0.29.0 personalised calendar (timetable, own events, day agenda), v0.30.0
friends on Firebase. Friends and the Drive backup need the Firebase project
(docs/FRIENDS_SETUP.md); 60 unit tests pass; nothing run on a phone yet.

## Active task (2026-11-11)
v0.27.0 released (themes, double class, Kanban board, weekday checklist, curriculum, security
sweep). Next: install on a phone and test; the deferred items are at the end of OPEN_ISSUES.md.

## Verified progress (2026-11-11)
- `./gradlew test assembleRelease` passes (JDK 17, SDK 34); 53 unit tests in core:model and
  core:data. Android Lint (`lintRelease`, every module): no errors.
- Release APK built with R8 minify + resource shrinking, signed with the debug key; every version
  is a GitHub release with `Attendo-vX.Y.Z.apk` and `Attendo.apk`
  (latest: https://github.com/madhukarverma/attendo/releases/latest/download/Attendo.apk).
- Navigation: tabs Home · Classes · Tasks (Checklist / Board / Reminders) · Timer · Stats;
  Settings from Home; Curriculum and Calendar are pushed screens.
- Data files: settings, subjects (lecture_hours), attendance (count), pomodoro, todos (reminders),
  habits, habit_log, kanban_columns, kanban_cards (.csv). Backup: CSV export/restore and the
  Google account backup.

## Not yet verified
- Nothing has been run on a phone or emulator; UI is checked only by compilation and lint.
- Google sign-in and Drive calls (needs the OAuth client first).
- Reminder delivery, board drag and drop, theme fonts on real hardware.

## Next actions
1. Install the latest APK and test each tab; set up the Google Cloud OAuth client.
2. Pick from the "Needs to be done" list in OPEN_ISSUES.md (friends, grades, personalised calendar).

## Last updated
2026-11-11
