# Open Issues / Room for Future Work

- ISSUE-001 — Mixed double class (present for one, absent for the other); today a ×2 mark is all present or all absent.
- ~~ISSUE-002 — Weekly timetable.~~ Done in v0.29.0.
- ISSUE-003 — Daily "mark today's attendance" reminder (to-do reminders exist since v0.18).
- ISSUE-006 — Release signing keystore + CI build (GitHub Actions). Also gives Google sign-in a
  stable SHA-1 (today it's the debug key of the build PC, DEC-012).
- ISSUE-007 — Home-screen widget.
- ISSUE-009 — Pomodoro: notification/alarm when a run ends while the app is in the background, and
  restoring a running timer after the system kills the app (needs a foreground service or AlarmManager).
- ISSUE-010 — Pomodoro: remember the chosen focus length and allow custom break lengths.
- ISSUE-008 — Bundle a grotesk + handwritten font (current: system sans / cursive).
- ISSUE-011 — Lab hours and syllabus (CA701, CA703) aren't in the curriculum PDF; add them when available.
- ISSUE-012 — Reminders: snooze, repeating reminders, and editing a to-do (today: add / tick / delete).
- ISSUE-013 — Reminders on Google Play would need SCHEDULE_EXACT_ALARM + a permission prompt (DEC-009).
- ISSUE-014 — Google backup: merge instead of replace when both phone and account have data;
  backup history (keep a few versions); a periodic backup with WorkManager when the app is closed.

## Needs to be done (requested, deferred on purpose — 2026-11-11)
- ISSUE-015 — Google account backup: built in v0.21; needs the shared Firebase/Google Cloud project
  (docs/FRIENDS_SETUP.md, step 5) and verification on a phone.
- ~~ISSUE-016 — Friends, LeetCode-style.~~ Built in v0.30.0 on Firebase; needs the Firebase project
  (docs/FRIENDS_SETUP.md) and a test with two accounts.
- ~~ISSUE-017 — Grades in previous assessments, private by default.~~ Done in v0.28.0.
- ~~ISSUE-018 — Personalised MCA calendar.~~ Done in v0.29.0 (timetable, own events, day agenda).
