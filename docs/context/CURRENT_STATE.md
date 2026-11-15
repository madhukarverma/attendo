# Current State

## Update 2026-11-15 (v0.36.0)
Apple look restored (system font, capsules), font picker, aligned tab titles, shared undo on
Classes, Tinder deck fitted above the tab bar, DNA mascots with idle animation. Checked against the
user's screenshots of v0.35.0; v0.36.0 itself not yet seen on a phone. Still to publish: the
v0.35.0 Firestore rules (feedback).

## Update 2026-11-15 (v0.35.0)
Feedback in Settings (Firestore, read in the console), design tokens applied app-wide, coded error
messages. Waiting on the user: publish the updated Firestore rules. The new look (font, corners,
spacing) hasn't been seen on a phone yet; fewer-cards work beyond Settings needs screenshots (ISSUE-026).

## Update 2026-11-15 (v0.34.0)
Security audit done (`docs/SECURITY.md`). Hardened Firestore rules with an emulator test suite
(`firebase/test/`), backup exclusions, newer Firebase and sign-in libraries, verified Gradle
download. Waiting on the user: publish the new rules, add the release SHA-1 in Firebase, and check
the API key's limits. The GitHub repo is private since 2026-11-15, so release links need a login. 70 unit tests and 32 rules tests
pass; still nothing tested on a phone.

## Update 2026-11-15 (v0.33.1)
Animation-performance pass: all progress bars and the card deck animate in the draw phase only;
the new animation-performance skill documents the rules. Still not measured on a device (no
frame-time numbers yet; ISSUE-022).

## Update 2026-11-15 (v0.33.0)
Release-signed with the new key (SHA-1 33:0C:B7:CA:21:A3:03:F8:D8:42:B7:16:52:59:9E:FB:9E:2B:D9:FA;
add it in Firebase). Dead code and duplicated controls removed. Apple-style motion: fluid card
swipes, critically damped springs, Apple's rubber band, frosted-glass tab bar, scroll-edge fades,
tuned type scale, shared haptics. 70 unit tests pass; nothing run on a phone yet, so the glass
blur, gestures and haptics are unverified on hardware.

## Update 2026-11-15
v0.31.0 moved the syllabus into the Focus tab and redesigned the timer. v0.32.0 fixes everything
from the code scan except the release keystore (ISSUE-006): the automatic Google backup now
watches every data file; signing in no longer restores over a phone that only has a timetable,
events or syllabus ticks; an unreadable data file is copied aside before anything is saved over
it, and a failed save shows a message instead of crashing. Friends: search returns at most 20
students, friend codes are checked against the account, and **Delete profile** removes you from
Friends. Targets Android 15 (SDK 35). 66 unit tests pass; nothing run on a phone yet. The updated
`firebase/firestore.rules` must be published in the Firebase console.

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
