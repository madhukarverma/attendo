# core/model/

Plain-Kotlin data types and the maths on them. Sources are in
`src/main/kotlin/com/attendo/core/model/`, tests in `src/test/kotlin/com/attendo/core/model/`.

| File | What it is | Open it for | Test |
|---|---|---|---|
| `Subject.kt` | A subject (name, code, target %, lecture hours) | subject fields | |
| `Attendance.kt` | Attendance status and one day's record | what a mark is | |
| `AttendanceSummary.kt` | Percentage, classes needed, classes you can miss | attendance maths | `AttendanceSummaryTest` |
| `CourseCatalog.kt` | The programme's subjects, used to pre-fill subjects | the MCA subject list | `CourseCatalogTest` |
| `Grades.kt` | Assessments, per-subject and overall grades, letter grades | grade maths | `GradesTest` |
| `Habit.kt` | Habits, check-ins, weekly streaks | habit maths | `HabitWeekTest` |
| `Kanban.kt` | Board columns, cards, priorities, moves | board rules | `KanbanOpsTest` |
| `Todo.kt` | Reminders and their agenda grouping | reminder data | `TodoAgendaTest` |
| `Pomodoro.kt` | Session kinds, finished sessions, focus stats, `formatClock` (mm:ss countdown) | focus stats, timer text | `PomodoroStatsTest` |
| `AcademicCalendar.kt` | Day kinds, calendar events, the academic calendar | holidays, exams, teaching days | |
| `Planner.kt` | Timetable slots, personal events, merging them into the calendar | timetable rules | `PlannerTest` |
| `Curriculum.kt` | Syllabus courses and units, coverage progress | syllabus progress | (in `core/data`: `CurriculumTest`) |
| `AppSettings.kt` | All settings, theme mode, animation mode, font choice | adding a setting | |
| `Physics.kt` | Apple's flick projection and rubber band, in px | gesture maths | `PhysicsTest` |
| `Nickname.kt` | The short name the app calls the user | greeting name | `NicknameTest` |
| `ErrorCode.kt` | Every user-facing error with its stable code (`ST-`, `IM-`, `BK-`, `FR-`, `FB-`); list in `docs/ERROR_CODES.md` | showing or adding an error | `FeedbackTest` |
| `Feedback.kt` | Feedback and rating, categories, validation, `FeedbackSender` interface, results | the feedback form | `FeedbackTest` |

## Rules
- Plain Kotlin only: no Android or Compose imports, so everything here runs in fast unit tests.
- Every new rule or calculation gets a unit test in the matching test file.
- A field added here that is saved also needs its CSV column (see `core/data/CLAUDE.md`).
