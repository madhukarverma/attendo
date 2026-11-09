# Changelog

## 2026-11-09 — v0.6.0 Pomodoro
- `:feature:pomodoro` with Timer / Stats tabs (segmented control), opened from a new bottom-bar button.
- `PomodoroTimer` (core:data): wall-clock based, app-scoped; focus 15/25/45/60, 5 min short break,
  15 min long break after 4 focus sessions; pause / resume / reset / skip; per-subject focus.
- Sessions saved to `pomodoro.csv` (completed runs, and focus runs stopped after ≥ 60 s).
- Stats: today / week / all-time focus, sessions, streak, 7-day chart, per-subject bars, recent list.
- Export pomodoro.csv from the Stats tab; Settings export now includes it.
- Haptic on finish; screen kept on while running. Unit tests for timer and stats.

## 2026-11-09 — v0.5.0 mood mascot
- Pentagon bounces (green) when on track, sways (slate blue) when below target, breathes when neutral.

## 2026-11-09 — v0.4.0 course catalog
- `CourseCatalog` with the semester's 7 subjects (CA711, CA713, CA715, CA717, CA719, CA701 lab, CA703 lab).
- Onboarding checkbox (on by default) adds them; empty Home shows a one-tap "Add my semester subjects".
- Add Subject screen offers not-yet-added course subjects as tap-to-fill suggestions.
- `SubjectRepository.addFromCatalog` skips codes that already exist (single CSV write).

## 2026-11-09 — v0.3.0 logo splash
- Original flat-polygon brand logo (`AttendoLogo`) and a ~1.5 s launch splash (`:feature:splash`).

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
