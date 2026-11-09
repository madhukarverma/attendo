# Current State

## Active task
v0.19.0 released. Next: real-device feedback (reminders, tab bar, themes, animation modes).

## Verified progress (2026-11-09)
- `./gradlew test assembleRelease` passes (JDK 17, SDK 34); 31 unit tests (attendance maths incl.
  lecture hours, nickname, to-do agenda, CSV codec, repositories incl. todos.csv, calendar, timer).
- Release APK built with R8 minify + resource shrinking, signed with the debug key; every version is
  a GitHub release with `Attendo-vX.Y.Z.apk` and `Attendo.apk`
  (latest: https://github.com/madhukarverma/attendo/releases/latest/download/Attendo.apk).
- Navigation: tabs Home (summary) · Classes (attendance) · To-do · Timer · Stats; Settings from Home.
- Data files: settings.csv, subjects.csv (incl. lecture_hours), attendance.csv, pomodoro.csv, todos.csv.

## Not yet verified
- Nothing has been run on a phone or emulator; UI is checked only by compilation.
- Reminder delivery (exact alarms, Doze, OEM battery savers), notification permission flow,
  re-arming after reboot.
- Dark theme contrast fix (v0.15) and the custom Recomposer / refresh-rate request on hardware.

## Next actions
1. Install the latest APK and test: set a reminder 2 minutes ahead, lock the phone, check it fires.
2. Enter lecture hours for each course (or share them so the catalog can pre-fill them).
3. Pick items from `OPEN_ISSUES.md`.

## Last updated
2026-11-09
