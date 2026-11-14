# Attendo — test report

Generated 2026-11-15 from commit `87e6246` (v0.31.0). Every test was re-run for this report
with `./gradlew :core:model:test --rerun :core:data:test --rerun` (JDK 17, Windows 11).

## Summary

| | |
|---|---|
| Automated tests | **62 / 62 passed**, 0 failed, 0 skipped |
| Test suites | 15 (in `core:model` and `core:data`) |
| Test time | 1.6 s |
| Release build (R8 + resource shrinking) | Passed |
| Android Lint, all 16 Android modules | 0 errors, 8 warnings (see below) |
| Run on a phone or emulator | **Never** — no device testing has been done |

Run them yourself:

```bash
./gradlew test
```

Results land in `core/*/build/test-results/test/` (XML) and `core/*/build/reports/tests/` (HTML).

## Automated unit tests

| Suite | Module | Covers | Tests | Result |
|---|---|---|---|---|
| `AcademicCalendarTest` | core/data | Bundled academic calendar (NIT Trichy, July 2026): loading, dates, holidays, Penta's day lines. | 5 | ✅ all passed |
| `BackupArchiveTest` | core/data | The Google-backup zip format: round trip, path filtering, size/entry limits. | 3 | ✅ all passed |
| `CsvTest` | core/data | The CSV codec and file table: quoting, header-keyed reads, formula neutralising, crash recovery. | 4 | ✅ all passed |
| `CurriculumTest` | core/data | The bundled MCA Semester I curriculum and its topic/outcome parsing. | 2 | ✅ all passed |
| `PomodoroTimerTest` | core/data | The Pomodoro engine: pause/reset, recording sessions, break cycle. | 3 | ✅ all passed |
| `RepositoryTest` | core/data | Every CSV repository against real temp files: write, reload, migrate, import, restore. | 12 | ✅ all passed |
| `AttendanceSummaryTest` | core/model | Attendance maths: percentages, classes needed / can skip, double classes, lecture hours, syllabus progress. | 10 | ✅ all passed |
| `CourseCatalogTest` | core/model | The CA course catalog: codes, short names, labs, missing-subject detection. | 3 | ✅ all passed |
| `GradesTest` | core/model | Grades maths: totals, percentages, letter grades, number formatting. | 2 | ✅ all passed |
| `HabitWeekTest` | core/model | Weekday checklist maths: weeks, due tasks, progress, streaks. | 4 | ✅ all passed |
| `KanbanOpsTest` | core/model | Board ordering rules: moving cards and columns, open/overdue cards. | 5 | ✅ all passed |
| `NicknameTest` | core/model | The nickname rule for names longer than 10 characters. | 2 | ✅ all passed |
| `PlannerTest` | core/model | Timetable and personal calendar: day view, next class, overlaps, merging events. | 3 | ✅ all passed |
| `PomodoroStatsTest` | core/model | Focus statistics: totals per day/week/subject and the streak. | 2 | ✅ all passed |
| `TodoAgendaTest` | core/model | Reminders/to-do grouping: overdue, today, upcoming, done; pending reminders. | 2 | ✅ all passed |

### Every test case

**AcademicCalendarTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `mascotLineMentionsUpcomingEventsAndStaysQuietOtherwise` | Penta warns about Diwali 2 days ahead and says nothing on an ordinary Thursday. | ✅ | 0.071 s |
| `stripCoversWholeSemester` | The day strip runs from 13 Aug (classes start) to 17 Dec (end of finals). | ✅ | 0.015 s |
| `multiDayEventsAndHolidays` | 2 Oct is a holiday; a 3-day assessment is found on its middle day and Penta says "day 2 of 3". | ✅ | 0.005 s |
| `bundledCalendarLoadsSemesterBounds` | The bundled CSV loads 31 events: classes 13 Aug – 25 Nov 2026, semester ends 17 Dec. | ✅ | 0.002 s |
| `weekendsAreHolidays` | Saturdays and Sundays are holidays; the semester has 70 class days. | ✅ | 0.003 s |

**BackupArchiveTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `entriesWithPathsAreIgnored` | Zip entries with paths ("../evil.csv", "dir/x.csv") are ignored (no zip slip). | ✅ | 0.015 s |
| `tooManyEntriesAreRejected` | A zip with 100 entries is rejected (zip-bomb guard). | ✅ | 0.005 s |
| `packAndUnpackKeepFilesAndManifest` | Files (text and binary) and the manifest survive pack → unpack unchanged. | ✅ | 0.004 s |

**CsvTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `roundTripsSpecialCharacters` | Commas, quotes, new lines and empty fields survive encode → parse. | ✅ | 0.001 s |
| `formulasAreNeutralisedOnExportAndRestoredOnImport` | Cells starting with = + - @ get a leading apostrophe on export and lose it on import. | ✅ | 0.000 s |
| `interruptedWriteIsRecovered` | If a save was cut off after the old file was removed, the temp file is recovered. | ✅ | 0.019 s |
| `tableReadsByColumnName` | Rows are read by column name; a newer schema still reads older files. | ✅ | 0.004 s |

**CurriculumTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `topicsKeepBracketedListsTogether` | Topic splitting keeps "Lists (traversing, slices)" together and drops a leading "and". | ✅ | 0.003 s |
| `bundledCurriculumHasEverySemesterOneTheoryCourse` | CA711–CA719 load with 40 hours, units I–V and 3 outcomes each; catalog hours match. | ✅ | 0.016 s |

**PomodoroTimerTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `pauseKeepsRemainingAndShortResetIsNotRecorded` | Pausing keeps the remaining time; resetting after only 30 s records nothing (under the 60 s minimum). | ✅ | 0.248 s |
| `completedFocusIsRecordedAndMovesToBreak` | A finished 25-minute focus run is saved with its subject, and a break follows. | ✅ | 0.079 s |
| `longBreakAfterFourFocusSessions` | The fourth focus session is followed by a long break. | ✅ | 0.019 s |

**RepositoryTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `plannerSurvivesReloadAndMergesIntoTheCalendar` | Timetable slots and own events save, reload, validate kinds and merge into the calendar. | ✅ | 0.206 s |
| `catalogLectureHoursFillOnlyEmptyValues` | Official 40 lecture hours fill only empty values, once; a manual "Clear" sticks. | ✅ | 0.042 s |
| `plainTodosMoveToTheBoardOnce` | Old to-dos without a time move to board cards once; reminders stay. | ✅ | 0.081 s |
| `dataSurvivesReload` | Subjects, attendance and settings (name, animation mode, theme, custom colours) reload from disk. | ✅ | 0.052 s |
| `gradesSurviveReloadAndImport` | Marks save, edit and reload; deleting a subject's marks works; grade CSVs import, bad rows dropped. | ✅ | 0.112 s |
| `habitsAndTicksSurviveReload` | Checklist tasks, weekdays and ticks reload; double ticks don't duplicate; delete removes history. | ✅ | 0.066 s |
| `kanbanBoardSurvivesReload` | Default columns are created once; cards move, keep notes/priority, and reload; deleting a column renumbers. | ✅ | 0.104 s |
| `addFromCatalogSkipsExistingCodes` | Adding the semester's subjects skips codes already added. | ✅ | 0.031 s |
| `clearingMarkRemovesRecord` | Re-marking replaces the day's mark; a double class (×2) is stored and reloads; clearing removes it. | ✅ | 0.032 s |
| `syllabusProgressSurvivesReload` | Covered syllabus units save (case-insensitive) and reload; unticking removes them. | ✅ | 0.041 s |
| `todosSurviveReload` | Reminders save with their time (seconds dropped), done flags reload, "clear done" works. | ✅ | 0.019 s |
| `csvImportMatchesTablesByHeaderAndReplaces` | CSV restore recognises files by columns (renamed, BOM, reordered), replaces only those tables, and a full restore empties missing ones. | ✅ | 0.057 s |

**AttendanceSummaryTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `doubleClassCountsAsTwo` | A ×2 mark counts as two classes; cancelled classes don't count; other subjects are ignored. | ✅ | 0.031 s |
| `emptySubjectIsOnTrack` | With no classes held a subject is on track at 0%. | ✅ | 0.000 s |
| `aboveTargetCanSkip` | Above target, the number of classes that can still be missed is right. | ✅ | 0.002 s |
| `exactlyOnTarget` | Exactly at the target counts as on track (no rounding error). | ✅ | 0.000 s |
| `lecturesLeftNeedsCourseHours` | Without lecture hours there is no "lectures left" figure. | ✅ | 0.000 s |
| `syllabusProgressCountsKnownUnitsOnly` | Syllabus progress counts only units of courses in the curriculum. | ✅ | 0.022 s |
| `lectureHoursGiveLecturesLeftAndSkips` | Lecture hours give lectures left, how many can be missed, and the best final %. | ✅ | 0.000 s |
| `belowTargetNeedsClasses` | Below target, the number of classes needed in a row is right. | ✅ | 0.000 s |
| `moreClassesThanHoursLeavesNoneLeft` | More classes marked than lecture hours leaves 0 left, never negative. | ✅ | 0.000 s |
| `unreachableTargetIsNegative` | When the target can no longer be reached, the outlook is negative. | ✅ | 0.000 s |

**CourseCatalogTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `shortNamesAndLabs` | Short names (DSA, DLCO, initials, "… Lab") and lab detection. | ✅ | 0.023 s |
| `missingIgnoresAlreadyAddedCodesCaseInsensitively` | With CA719 already added (any letter case), 6 catalog subjects are still missing. | ✅ | 0.001 s |
| `catalogHasSevenSubjectsWithUniqueCodes` | The catalog has 7 subjects with unique codes, 2 of them labs. | ✅ | 0.001 s |

**GradesTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `totalsAndLetters` | Subject totals, percentages, newest-first order and S–F letter grades. | ✅ | 0.022 s |
| `formatAndBounds` | Marks print as "18" / "13.5"; letter boundaries; zero maximum is safe. | ✅ | 0.001 s |

**HabitWeekTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `streakStopsAtTheStartDate` | Days before a checklist task existed don't break its streak. | ✅ | 0.005 s |
| `weekRunsMondayToSunday` | A week is Monday to Sunday. | ✅ | 0.002 s |
| `streakCountsScheduledDaysOnly` | Streaks count scheduled days only; an unticked today doesn't break it. | ✅ | 0.000 s |
| `dueAndProgressFollowTheWeekday` | Tasks due and "done of total" follow the weekday. | ✅ | 0.005 s |

**KanbanOpsTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `moveToAnotherColumnRenumbersBoth` | Moving a card to another column renumbers both columns. | ✅ | 0.014 s |
| `moveIntoEmptyColumn` | A card can move into an empty column; next position is computed. | ✅ | 0.001 s |
| `moveColumnRenumbers` | Moving a column renumbers all columns. | ✅ | 0.011 s |
| `reorderWithinColumnAndClampIndex` | Reordering within a column clamps out-of-range positions; unknown cards are a no-op. | ✅ | 0.000 s |
| `boardKnowsOpenAndOverdueCards` | Overdue is flagged except in the "done" column; due-soon order; next column. | ✅ | 0.018 s |

**NicknameTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `shortNamesAreKept` | Names of 10 characters or fewer are kept. | ✅ | 0.003 s |
| `longNamesBecomeFirstFourLetters` | Longer names become their first 4 letters ("Prabhanshu Kumar" → "Prab"). | ✅ | 0.001 s |

**PlannerTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `personalEventsMergeIntoTheCalendar` | Own events merge into the academic calendar once, marked personal. | ✅ | 0.018 s |
| `dayAndNext` | Classes for a day in time order, and the next / in-progress class. | ✅ | 0.003 s |
| `overlapsNeedSameDayAndTime` | Two classes overlap only on the same day with overlapping times. | ✅ | 0.001 s |

**PomodoroStatsTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `summarizesFocusTime` | Today (35), week (60) and total (110) focus minutes, completed sessions, streak and the 7-day chart. | ✅ | 0.017 s |
| `streakCountsFromYesterdayWhenTodayIsEmpty` | The focus streak counts from yesterday when nothing is done today yet. | ✅ | 0.000 s |

**TodoAgendaTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `pendingRemindersAreFutureAndOpen` | Only future, not-done reminders are pending, soonest first. | ✅ | 0.011 s |
| `groupsByDayAndStatus` | To-dos group into overdue / today / upcoming / done in the right order. | ✅ | 0.010 s |

## Other checks attempted

| Check | How | Result |
|---|---|---|
| Release build | `./gradlew assembleRelease` (R8 minify, resource shrinking) after every feature, v0.1.0–v0.31.0 | Passed (APK 3.3 MB) |
| Android Lint | `./gradlew lintRelease` on all 16 Android modules | 0 errors; warnings: portrait lock (intentional, ×2), `enableOnBackInvokedCallback` on old Android, unused `R.color.green`, unneeded `mipmap-anydpi-v26`, modifier parameter order, `CredentialDependency` (false positive: the dependency is present in `core:social`), one `mutableIntStateOf` hint |
| APK inspection | `aapt dump badging`, `unzip -l` | Not debuggable; package `com.attendo.app`, minSdk 26, targetSdk 34; no config files or keys inside |
| Secrets | Searched tracked files and the whole git history for API keys, private keys, client secrets, tokens, keystore passwords | None found; `google-services.json`, `firebase.properties`, `local.properties` are git-ignored |
| Exported components / intents | Manifest and code review | Only the launcher activity is exported; all 3 `PendingIntent`s are explicit and `FLAG_IMMUTABLE` |
| Risky APIs | Search for logging, `runBlocking`, `GlobalScope`, WebView, custom TLS, world-readable files | None |
| Firebase config parsing | Built once with a dummy `google-services.json`, then with the real one | All 4 values (project, app id, API key, web client id) reach `BuildConfig`; empty when the file is absent |
| Module rules | Checked `build.gradle.kts` of every feature | No feature module depends on another |

## Failures found while testing (all fixed)

| When | What failed | Cause | Fix |
|---|---|---|---|
| v0.11 calendar | `AcademicCalendarTest` expected 69 class days | Wrong expectation (5 weekday holidays, not 6) | Expected 70 |
| v0.11 calendar | "Ordinary day" test picked Sat 10 Oct | A weekend now has its own line | Used Thu 8 Oct |
| v0.27 security | `CsvTest.formulasAreNeutralised…` | The test's own CSV wasn't valid (unescaped quotes) | Quoted the cell as real exports do |
| Several releases | Compile errors caught by the build (wrong argument order, a private type exposed, a parameter added to the wrong screen) | Coding mistakes | Fixed before release; no release shipped broken |
| Several releases | `verifyReleaseResources` "file in use" | Windows locking a fresh file (antivirus/indexer) | Re-ran; not a code problem |

## Not tested

- **No device or emulator runs.** Every screen, animation, theme, gesture (Tinder swipes, board drag and drop), the launch screen and the custom animation clock are checked only by compiling and Lint.
- **No UI or instrumented tests**, and no tests for the 16 modules outside `core:model` / `core:data` (ViewModels, `core:backup`, `core:social`, `app`).
- **Not exercised against real services:** Google sign-in, Google Drive backup/restore, Firebase Auth, Firestore reads/writes and the Firestore security rules (no emulator run).
- **Not verified on hardware:** reminder notifications and exact alarms (Doze, OEM battery savers, reboot), notification permission prompts, the high-refresh-rate request.

## Open issues found by the code scan (2026-11-15)

1. **High** — the automatic Google backup only watches 6 of 13 data tables (grades, checklist, board, timetable, events and syllabus edits aren't backed up until something else changes).
2. **Medium** — Firestore rules let any signed-in student list every profile with no limit.
3. **Medium** — a failed CSV write (e.g. storage full) crashes the app.
4. **Medium** — backup sign-in treats a phone with only a timetable/events/syllabus as empty and restores over it.
5. **Medium** — targetSdk 34 is below Google Play's API 36 requirement; releases use the debug key.

Each of these deserves a regression test when it is fixed.
