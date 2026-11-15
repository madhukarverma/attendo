# Attendo

Kotlin + Jetpack Compose Android app for one MCA student: attendance, tasks, focus timer, grades,
calendar and friends. All data is CSV. Private repo `madhukarverma/attendo`, branch `master`.

## Map: where things live

| Folder or file | What's inside | Open it when |
|---|---|---|
| `app/` | Start-up and app container, navigation and tabs, reminders, CSV export/import, launch image, manifest. See `app/CLAUDE.md` | wiring a screen in, notifications, permissions |
| `app/build.gradle.kts` | Version number, release signing (from `keystore.properties`), Firebase settings read from `google-services.json` | releasing, changing build settings |
| `core/model/` | Plain-Kotlin data types and maths: attendance, grades, habits, board, planner, curriculum, gesture physics. See `core/model/CLAUDE.md` | changing a calculation or a data type |
| `core/data/` | CSV storage and repositories, bundled calendar and curriculum CSVs, backup format, CSV import, Pomodoro timer engine. See `core/data/CLAUDE.md` | changing what's saved, adding a column or file |
| `core/designsystem/` | Themes, type scale, colours, motion and haptics, frosted glass, mascots, shared controls, tab bar. See `core/designsystem/CLAUDE.md` | look and feel, animations |
| `core/backup/` | Google Drive backup: `GoogleDriveBackup.kt` (sync, change watcher), `DriveAppDataClient.kt` (HTTP), `GoogleSignIn.kt` | backup problems |
| `core/social/` | Friends backend on Firebase: `SocialService.kt` (sign-in, requests, sharing), `SocialModels.kt` | friends logic, what's shared |
| `feature/` | One module per screen, 12 in all. See `feature/CLAUDE.md` | changing a screen |
| `firebase/firestore.rules` | Server-side privacy rules for friends | changing who sees what (re-publish in the Firebase console) |
| `firebase/test/` | Tests for those rules on the local Firestore emulator (`npm install`, `npm test`; Node + Java) | after any rules change, before publishing |
| `docs/` | Setup guides, security notes, test report, flowcharts, project memory. See `docs/CLAUDE.md` | setup, history, decisions, recording a feature |
| `gradle/libs.versions.toml` | Every library and plugin version | adding or updating a library |
| `settings.gradle.kts` | Module list | adding a module |
| `build.gradle.kts`, `gradle.properties` | Root build setup | rarely |
| `firebase.properties.example` | Template for Firebase settings when there is no `google-services.json` | setting up Firebase on another machine |
| `keystore.properties.example` | Template for the release key's path and password (`keystore.properties`, never committed) | building releases on another machine |
| `README.md` | The GitHub page: plain-language features, flowcharts, charts, install steps; developer notes and the data file list folded at the end | changing features, data files or install steps |
| `.claude/skills/apple-design/SKILL.md` | The user's Apple design skill (springs, gestures, materials, type), written in web terms | motion, gestures or visual polish (Compose version: `core/designsystem/CLAUDE.md`) |
| `.claude/skills/animation-performance/SKILL.md` | Keeping animations cheap: draw-phase reads, no layout per frame, loops, blur, measuring | anything that moves, or jank and battery problems |

## Rules

**Routing**
- Read the map first and open only the file the task needs.
- Never scan a whole folder when the map says where to look.
- When a folder or important file is added, moved or renamed, update the map in the same step
  (this file and the folder's own `CLAUDE.md`).

**Code**
- All user data lives in CSV files in app storage. No database, no other format.
- Feature modules never depend on each other; the app passes shared pieces in as composable slots.
- `app/google-services.json`, `firebase.properties`, `keystore.properties` and the `.jks` key are never committed.

**Workflow**
- One `feature/*` branch per feature (`docs/*` for docs only). Push it, merge `--no-ff` into
  `master`, push.
- After every feature: bump `versionCode` and `versionName`, build, publish a GitHub release with
  `Attendo-vX.Y.Z.apk` and `Attendo.apk`, and give the user the links. Latest download:
  https://github.com/madhukarverma/attendo/releases/latest/download/Attendo.apk (needs a GitHub
  login with access to the repo).
- Update `docs/context/` with each feature (see `docs/CLAUDE.md`).
- PowerShell 5.1: pass commit messages with `git commit -F <file>`. Never round-trip Kotlin files
  through `Get-Content`/`Set-Content` (it breaks UTF-8).
- Build and test from Bash:
  `export JAVA_HOME="$USERPROFILE/android-dev/jdk-17.0.21+9" && ./gradlew.bat test assembleRelease`.
  The APK lands in `app/build/outputs/apk/release/app-release.apk`. Only publish it when the
  build didn't warn about the debug key (`docs/RELEASE_SIGNING.md`).

## Common tasks

Paths are shortened: `core/model/…/X.kt` means `core/model/src/main/kotlin/com/attendo/core/model/X.kt`,
and `core/model/src/test/…/X.kt` is the same path under `src/test`.

1. **Change attendance maths:** `core/model/…/AttendanceSummary.kt` → `feature/attendance/…/AttendanceViewModel.kt`
   → `feature/attendance/…/AttendanceScreen.kt` → `core/model/src/test/…/AttendanceSummaryTest.kt`.
2. **Add a column to a CSV file:** the type in `core/model/…` → its repository in `core/data/…`
   (see `core/data/CLAUDE.md`) → the file list comment in `core/data/…/AttendoData.kt`
   → `core/data/…/backup/CsvImport.kt` → `core/data/src/test/…/RepositoryTest.kt`.
3. **Add a new data file:** `core/data/…/Repositories.kt` → a new `core/data/…/<area>/Csv<Name>Repository.kt`
   → `core/data/…/AttendoData.kt` (every spot on the checklist in `core/data/CLAUDE.md`) → `core/data/…/backup/CsvImport.kt`
   → `core/data/src/test/…/RepositoryTest.kt`. The Google backup picks it up through `AttendoData.changes`.
4. **Add a screen:** `settings.gradle.kts` → `feature/<name>/build.gradle.kts` (copy a sibling) → the screen
   → `app/build.gradle.kts` (dependency) → `app/…/navigation/AttendoNavHost.kt` (`Routes` and the composable).
5. **Add or change a theme:** `core/designsystem/…/theme/Theme.kt` → `core/model/…/AppSettings.kt`
   → `feature/settings/…/ThemeSection.kt`.
6. **Change animations or gestures:** `core/designsystem/…/motion/Motion.kt` → `core/model/…/Physics.kt` (flick projection,
   rubber band) → `motion/IosPress.kt`, `motion/BounceScroll.kt` or `motion/Haptics.kt`
   → `app/…/navigation/Transitions.kt`.
7. **Change the tabs or navigation:** `app/…/navigation/AttendoNavHost.kt` → `core/designsystem/…/component/TabBar.kt`.
8. **Change the timer:** `feature/pomodoro/…/FocusDial.kt` (the dial) or `PomodoroScreen.kt` (controls, pages)
   → `core/data/…/pomodoro/PomodoroTimer.kt` (timing) → `core/data/src/test/…/PomodoroTimerTest.kt`.
9. **Change reminders:** `feature/tasks/…/TodoScreen.kt` → `app/…/reminders/ReminderScheduler.kt`,
   `ReminderReceiver.kt`, `ReminderNotifications.kt` → `app/src/main/AndroidManifest.xml`.
10. **Change friends:** `core/social/…/SocialService.kt` → `firebase/firestore.rules` → `firebase/test/rules.test.mjs`
    (`npm test`) → `feature/friends/…/FriendsScreen.kt`, `StudentScreen.kt`.
11. **Change the Google backup:** `core/backup/…/GoogleDriveBackup.kt` → `feature/settings/…/GoogleBackupSection.kt`.
12. **Release a version:** `app/build.gradle.kts` → `docs/context/CHANGELOG.md` → build and test → tag `vX.Y.Z`
    → `gh release create` with both APK names.
13. **Record a finished feature:** `docs/context/CHANGELOG.md`, `CURRENT_STATE.md`, `OPEN_ISSUES.md`,
    `DECISIONS.md` (only if a choice was made), and `docs/FLOWCHART.md` if screens or navigation changed.
