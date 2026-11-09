# Current State

## Active task
v0.1.0 feature-complete (bare bones). Next: refinement on real-device feedback.

## Verified progress (2026-11-09)
- `./gradlew test assembleDebug assembleRelease` passes (JDK 17, SDK 34).
- Unit tests: attendance maths (`core:model`), CSV codec + repositories reload from disk (`core:data`).
- Release APK built with R8 minify + resource shrinking (~1.2 MB), signed with the debug key.
- Merged to `master`: base, feature/subjects, feature/attendance, feature/subject-detail,
  feature/stats, feature/settings.

## Not yet verified
- Not run on a physical device or emulator in this session; UI checked only by compilation.

## Next actions
1. Install `Attendo-v0.1.0.apk` on a phone and collect UI feedback.
2. Pick items from `OPEN_ISSUES.md` (timetable, multiple lectures/day, reminders, CSV import).

## Last updated
2026-11-09
