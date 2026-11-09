# Changelog

## 2026-11-09 — v0.21.0 Google account backup
- New `:core:backup` module (`GoogleDriveBackup`): Google sign-in via Play services
  `AuthorizationClient` for the `drive.appdata` scope, and a small Drive v3 REST client. All CSV
  files plus the launch image are stored as one `attendo-backup.zip` (with `manifest.csv`) in the
  account's hidden app-data folder; only the newest copy is kept.
- Settings › Backup › Google account: sign in, last backup time, automatic backup a few seconds
  after every change (switch), Back up now, Restore (confirm), Sign out (revokes access, the backup
  stays). At sign-in: no backup → upload; empty phone → restore; both → the user chooses.
- Welcome screen: "Used Attendo before? Restore from Google" restores and skips set-up.
- Needs a one-time Google Cloud OAuth client for `com.attendo.app` + the signing SHA-1:
  `docs/GOOGLE_BACKUP_SETUP.md`. Adds the INTERNET permission.

## 2026-11-09 — v0.20.0 restore from CSV
- Settings › Backup › CSV files: Export (as before) and Restore. Restore opens the system file
  picker (several files at once), matches each file to a table by its header row (renamed files,
  Excel's BOM/CRLF and reordered columns are fine), shows what will be replaced and how many rows,
  then replaces only those tables. Rows that don't decode are skipped.
- Data layer: `AttendoData.planCsvImport` / `applyCsvImport`, plus `snapshot` / `restoreSnapshot`
  (full replace) for the coming account backup; `CsvStore.replaceRows`.
- Launch screen no longer waits forever if settings say there's a launch image but the file is gone.

## 2026-11-09 — v0.19.0 Home summary + tab bar
- New `:feature:dashboard` = Home tab and start screen: greeting with the nickname, attendance card
  (overall %, subjects below target, today's marking, lectures left, Penta's mood), Pomodoro card
  (live countdown with start/pause, or today's focus and streak), today's to-dos (tick off in
  place, overdue count), upcoming reminders, upcoming exams from the academic calendar. Settings
  moved to the gear on Home.
- One app-wide tab bar (`AttendoTabBar`): Home · Classes · To-do · Timer · Stats. The selected tab
  is the green pill with its label; the bar hides on pushed screens. Standard bottom-nav back stack
  (one copy of each tab, state saved, Back returns to Home). Tab switches: instant in iOS mode,
  cross-fade in Smooth, fade-through in Android, none in None.
- `:feature:home` renamed to `:feature:attendance` (AttendanceRoute/Screen/ViewModel); its header
  reads "Classes" and the old pill bar component was removed. Pomodoro, Stats and To-do no longer
  show a back button (they are tabs). Picking a day on the calendar from Home opens Classes on it.

## 2026-11-09 — v0.18.0 to-do list and reminders
- New `:feature:todo` module: to-dos belong to a day; adding a time makes it a reminder. Sections
  Overdue / Today / Upcoming / Done (clearable). Stored in `todos.csv`
  (id,title,date,remind_at,done,created_at) and included in CSV export.
- Reminders: `ReminderScheduler` (app module) keeps one AlarmManager alarm per pending reminder,
  re-synced whenever todos.csv changes and after boot / app update / clock or time-zone changes.
  `ReminderReceiver` re-checks the file before notifying; the notification has "Mark done" and
  opens the To-do screen. Android 13+ notification permission is asked when the first reminder
  is set. Exact alarms via USE_EXACT_ALARM (SCHEDULE_EXACT_ALARM on 12/12L), inexact fallback.
- Academic calendar shows to-dos: ring marker on the day, "Your to-dos" card, and per-month rows.
- Bottom bar: the green button is now "To-do"; "Add a subject" moved under the subject list.

## 2026-11-09 — v0.17.0 lecture hours
- Subjects have optional total lecture hours (`lecture_hours` column in subjects.csv; 1 lecture =
  1 hour). Lectures left = hours - classes held (cancelled classes don't count).
- Subject detail: "Course hours" card (lectures left, progress, edit dialog) and an outlook line
  (how many of the remaining lectures can be missed and still finish at the target, or the best
  final percentage when the target is out of reach). Add-subject form takes the hours too; Home
  rows show "N left".

## 2026-11-09 — v0.16.0 nickname
- Names longer than 10 characters are shown as their first 4 letters ("Prabhanshu Kumar" ->
  "Prab") via `Nickname.of` / `AppSettings.nickname`. The full name stays in settings.csv;
  onboarding and Settings show the nickname that will be used.

## 2026-11-09 — v0.15.0 dark theme legibility
- Text and icons that didn't pass a colour fell back to black (Material 3 only sets
  `LocalContentColor` inside a Surface), so they vanished on the dark theme. `AttendoTheme` now
  provides the palette's text colour, and every Material colour slot (dialogs, pickers, menus)
  comes from the palette.
- Swipe-card text and the mascot face use fixed colours, since what they sit on doesn't change
  with the theme.

## 2026-11-09 — v0.14.0 launch screen, rotation lock, header, Penta
- Generated launch art removed; launch screen shows "MCA 2026-29", an optional user-picked image
  (Settings › Launch screen, system photo picker) on a background sampled from the image's edges,
  the wordmark and the developer credit. Android 12+ system splash no longer shows an icon.
- App locked to portrait.
- Home header top padding 24 → 14 dp, list spacing 10 → 8 dp (constants in HomeScreen.kt).
- Penta (pentagon mascot, name is a code-only easter egg) is 156 dp; mood = in the clear (all
  subjects at/above target) → happy, else sad, independent of the day. Bubble alternates between
  the day's significance and the attendance warning only when not in the clear.

## 2026-11-09 — v0.13.0 themes
- Light / Dark (pitch black + dark grey) / Custom (11 hex colours); stored as `theme`, `color.<key>`.

## 2026-11-09 — v0.12.0 animation modes
- Smooth / iOS / Android / None slider (`animation_mode`); Apple spring model for iOS; high refresh
  rate in Smooth and iOS; None removes transitions, motion and the launch screen.

## 2026-11-09 — v0.11.0 academic calendar screen
- Calendar button beside the date; month grids, legend, coming-up list; weekends are holidays.

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
