# Changelog

## 2026-11-09 — v0.10.0 animations switch + launch screen
- Settings "Animations" switch (on by default, key `ios_motion`). Off = every animation instant:
  `AppMotionScale` (MotionDurationScale) is installed in the activity's own Recomposer, nav
  transitions become None, rubber-band bounce and the pentagon's idle loop are disabled, no ripples.
- Android stretch overscroll removed app-wide.
- New launch screen on #1A1D1A (the colour the user's reference image uses at its edges) with an
  original flat-vector portrait (`LaunchArt`), wordmark and a greyed "Developed by @madhukarverma"
  credit; Android 12+ system splash uses the same colour. Credit also in Settings › About.

## 2026-11-09 — v0.9.0 UI tweaks
- Tinder mode hides the mascot; month separators in the day strip; selected date in the header.
- Cards use short names on distinct solid colours; sad pentagon light red; Pomodoro without bubble,
  short subject names, no labs.

## 2026-11-09 — v0.8.0 academic calendar
- Bundled `core/data/src/main/resources/calendar/academic_calendar.csv` transcribed from the official
  NIT Trichy PG/MCA (I) July 2026 calendar PDF: classes 13 Aug – 25 Nov, finals to 17 Dec, holidays,
  assessments, CCM deadlines, Sportsfete, Festember.
- Home day strip now spans the whole semester with coloured dots (pink holiday, blue exam,
  green fest/milestone, yellow deadline).
- Mascot bubble talks about the selected day (holiday, assessment day N of M, deadlines, first/last
  day of class) and gives a heads-up 1–3 days before an event.

## 2026-11-09 — v0.7.0 Tinder mode
- Home switch shows swipeable subject cards: right present, left absent, down no class; undo.

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
