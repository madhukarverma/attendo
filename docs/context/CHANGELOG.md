# Changelog

## 2026-11-09 — v0.2.0 iOS-style motion
- Push/pop transitions with 30% parallax and dimming; swipe-back via Android predictive back.
- Add Subject presented as a sheet: slides up, screen behind scales back.
- Rubber-band overscroll with fling bounce on every scrolling screen.
- Collapsing large titles with "‹ Home" back button on Stats, Settings and Subject detail.
- Press feedback: row highlight, button dim, spring scale on chips/day pills; haptic tick on marking.
- Animated list insert/remove, animated percentages and stats bars.
- Settings toggle "iOS-style motion" (saved as `ios_motion` in settings.csv); off = Material defaults.

## 2026-11-09 — v0.1.0
- feature/settings: change name, export all CSV files through the Android share sheet (FileProvider).
- feature/stats: overall card + per-subject progress bars with target marker.
- feature/subject-detail: percentage, can-skip / must-attend tips, history, delete with confirmation.
- feature/attendance: Present / Absent / No class per day, live percentages, mascot mood.
- feature/subjects: add subject (name, code, target %), list on home.

## 2026-11-09 — base framework
- Multi-module Gradle project (version catalog, JDK 17, AGP 8.5.2).
- `:core:model` with attendance maths + unit tests.
- `:core:data` CSV storage (settings/subjects/attendance) + unit tests.
- `:core:designsystem` theme, mascots (pentagon, triangle, blob), speech bubble, day strip, pill bar.
- `:feature:onboarding`, `:feature:home` shell, navigation in `:app`.
