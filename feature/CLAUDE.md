# feature/

One Gradle module per screen. Each module's sources are in
`<module>/src/main/kotlin/com/attendo/feature/<module>/`. How screens connect is in
`app/src/main/kotlin/com/attendo/app/navigation/AttendoNavHost.kt`.

| Module | Where it shows | Files (main file first) | Open it for |
|---|---|---|---|
| `dashboard` | Home tab | `DashboardScreen.kt`, `DashboardViewModel.kt` | Home cards, next class, greeting |
| `attendance` | Attendance tab | `AttendanceScreen.kt`, `AttendanceViewModel.kt`, `SwipeDeck.kt` (swipe cards to mark) | marking attendance |
| `tasks` | Tasks tab | see below | checklist, board, reminders |
| `pomodoro` | Focus tab | `PomodoroScreen.kt` (pages, controls), `FocusDial.kt` (animated dial), `PomodoroViewModel.kt` (stats) | timer, Focus tab layout |
| `stats` | Stats tab | `StatsScreen.kt` (view model inside) | charts, grades card |
| `curriculum` | Syllabus page of the Focus tab, and on each subject page | `Syllabus.kt` | syllabus look and progress |
| `subjects` | Pushed screen and sheet | `SubjectDetailScreen.kt`, `AddSubjectScreen.kt`, `GradesSection.kt` | a subject's page, adding subjects, grades |
| `calendar` | Pushed screens | `CalendarScreen.kt` (month view), `PlannerUi.kt` (day agenda, event dialog, timetable screen) | calendar, timetable, personal events |
| `friends` | Pushed screens | `FriendsScreen.kt` (search, requests, list), `StudentScreen.kt` (a friend's page) | friends screens |
| `settings` | Pushed screen | `SettingsScreen.kt`, `ThemeSection.kt`, `AnimationModeSlider.kt`, `CsvBackupSection.kt`, `GoogleBackupSection.kt`, `LaunchImageSection.kt` | any setting |
| `onboarding` | First launch | `OnboardingScreen.kt` (view model inside) | first-run flow |
| `splash` | Each launch (skipped when Animations is None) | `LogoSplash.kt` | launch animation |

**`tasks`** has three pages, chosen by the segmented control in `TasksScreen.kt` (`TasksPage`):

| Page | Screen | View model | Extra |
|---|---|---|---|
| Checklist (habits) | `HabitsPage.kt` | `HabitsViewModel.kt` | |
| Board | `BoardPage.kt` | `BoardViewModel.kt` | `BoardDialogs.kt` (card and column editors) |
| Reminders | `TodoScreen.kt` | `TodoViewModel.kt` | |

## Rules
- A feature module never depends on another feature module. When a screen needs a piece of
  another feature, the app passes it in as a composable slot or callback.
- Screens are built in three layers: `XRoute` (creates the view model, collects its `StateFlow`)
  → `XViewModel` (reads and writes repositories) → `XScreen` (draws state, no data access).
- Tab screens leave `TabBarClearance` of space at the bottom for the tab bar.
- Use the shared controls in `core/designsystem/.../component/Controls.kt` (pills, pickers, field
  colours, hero stats) rather than a private copy.
- A new module also goes in `settings.gradle.kts`, `app/build.gradle.kts` and the map in the root
  `CLAUDE.md`.
