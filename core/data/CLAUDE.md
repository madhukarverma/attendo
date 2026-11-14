# core/data/

Everything that is saved. Sources are in `src/main/kotlin/com/attendo/core/data/`, tests in
`src/test/kotlin/com/attendo/core/data/`.

| File | What it is | Open it for |
|---|---|---|
| `AttendoData.kt` | Entry point: builds every repository, lists every CSV file and its columns, backup/restore/import, one-time migrations | adding a data file, restore, migrations |
| `Repositories.kt` | Every repository interface (what screens may read and change) | adding or changing a data operation |
| `CsvRepositories.kt` | Repositories for `settings.csv`, `subjects.csv`, `attendance.csv` | settings, subjects, attendance marks |
| `grades/CsvGradeRepository.kt` | `grades.csv` | grades |
| `planner/CsvPlannerRepository.kt` | `timetable.csv` and `events.csv`, merged with the academic calendar | timetable, personal events |
| `curriculum/CsvSyllabusRepository.kt` | `syllabus.csv` (units marked as covered) | syllabus progress |
| `curriculum/CurriculumLoader.kt` | Reads the bundled syllabus CSV | syllabus content loading |
| `calendar/AcademicCalendarLoader.kt` | Reads the bundled academic calendar CSV | holidays, exam dates loading |
| `habit/CsvHabitRepository.kt` | `habits.csv` and `habit_log.csv` | habits |
| `kanban/CsvKanbanRepository.kt` | `kanban_columns.csv` and `kanban_cards.csv` | the board |
| `todo/CsvTodoRepository.kt` | `todos.csv` (reminders) | reminders data |
| `pomodoro/CsvPomodoroRepository.kt` | `pomodoro.csv` (finished sessions) | focus history |
| `pomodoro/PomodoroTimer.kt` | The timer engine: config, state, ticking, session switching | timer behaviour |
| `csv/Csv.kt` | CSV encoder/parser and formula-injection protection | parsing bugs, CSV safety |
| `csv/CsvTable.kt` | One CSV file with a header; atomic writes | file reading/writing |
| `csv/CsvStore.kt` | In-memory copy of a table as a `StateFlow`, written through to disk; `StorageProblems` (read/save failures for the user) | how data reaches screens, storage errors |
| `backup/BackupArchive.kt` | The backup zip (CSV files plus `manifest.csv`) | backup file format |
| `backup/CsvImport.kt` | Matches picked CSV files to tables by header row | CSV import |

Bundled data (read-only, shipped in the APK) is in `src/main/resources/`:
`calendar/academic_calendar.csv` and `curriculum/mca_semester1.csv`.

Tests: `RepositoryTest` (all repositories), `CsvTest`, `BackupArchiveTest`, `AcademicCalendarTest`,
`CurriculumTest`, `PomodoroTimerTest`.

## Rules
- A new data file must be registered in all of these, in the same change:
  the file-name constant, `ALL_FILES`, the `stores` map and the file list comment in
  `AttendoData.kt`; `hasNoUserData()` if it holds user data; `CsvSchema` in `backup/CsvImport.kt`;
  the data file list in `README.md`. (The automatic Google backup follows `stores` through
  `AttendoData.changes`, so it needs nothing extra.)
- New columns go at the end. Old files without the column must still load, so decode a missing
  value as a sensible default.
- Bad rows are skipped when decoding, never crash the app. Disk errors go through
  `StorageProblems`, never as exceptions to the caller (DEC-018).
- Every repository change gets a case in `RepositoryTest`.
