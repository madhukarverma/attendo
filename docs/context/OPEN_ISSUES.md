# Open Issues / Room for Future Work

- ISSUE-001 — Multiple lectures of one subject on the same day (add `count`/`slot` column to attendance.csv).
- ISSUE-002 — Weekly timetable so the home screen only shows subjects scheduled that day.
- ISSUE-003 — Daily "mark today's attendance" reminder (to-do reminders exist since v0.18).
- ISSUE-006 — Release signing keystore + CI build (GitHub Actions). Also gives Google sign-in a
  stable SHA-1 (today it's the debug key of the build PC, DEC-012).
- ISSUE-007 — Home-screen widget.
- ISSUE-009 — Pomodoro: notification/alarm when a run ends while the app is in the background, and
  restoring a running timer after the system kills the app (needs a foreground service or AlarmManager).
- ISSUE-010 — Pomodoro: remember the chosen focus length and allow custom break lengths.
- ISSUE-008 — Bundle a grotesk + handwritten font (current: system sans / cursive).
- ISSUE-011 — Pre-fill lecture hours for the catalog subjects (CA701–CA719) once the official hours are known.
- ISSUE-012 — Reminders: snooze, repeating reminders, and editing a to-do (today: add / tick / delete).
- ISSUE-013 — Reminders on Google Play would need SCHEDULE_EXACT_ALARM + a permission prompt (DEC-009).
- ISSUE-014 — Google backup: merge instead of replace when both phone and account have data;
  backup history (keep a few versions); a periodic backup with WorkManager when the app is closed.
