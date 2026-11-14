# Attendo — test report

Generated 2026-11-15 from commit `ec099ab` (v0.33.0). Every test was re-run for this report
with `./gradlew :core:model:test --rerun :core:data:test --rerun` (JDK 17, Windows 11).

## Summary

| | |
|---|---|
| Automated tests | **70 / 70 passed**, 0 failed, 0 skipped |
| Test suites | 16 (in `core:model` and `core:data`) |
| Test time | 1.1 s |
| Release build (R8 + resource shrinking) | Passed |
| Android Lint (v0.33: the 9 changed Android modules) | 0 errors, 0 new warnings |
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
| `CsvTest` | core/data | The CSV codec and file table: quoting, header-keyed reads, formula neutralising, crash recovery, unreadable-file copies. | 5 | ✅ all passed |
| `CurriculumTest` | core/data | The bundled MCA Semester I curriculum and its topic/outcome parsing. | 2 | ✅ all passed |
| `PomodoroTimerTest` | core/data | The Pomodoro engine: pause/reset, recording sessions, break cycle. | 3 | ✅ all passed |
| `RepositoryTest` | core/data | Every CSV repository against real temp files: write, reload, migrate, import, restore, backup watcher, storage failures. | 15 | ✅ all passed |
| `AttendanceSummaryTest` | core/model | Attendance maths: percentages, classes needed / can skip, double classes, lecture hours, syllabus progress. | 10 | ✅ all passed |
| `CourseCatalogTest` | core/model | The CA course catalog: codes, short names, labs, missing-subject detection. | 3 | ✅ all passed |
| `GradesTest` | core/model | Grades maths: totals, percentages, letter grades, number formatting. | 2 | ✅ all passed |
| `HabitWeekTest` | core/model | Weekday checklist maths: weeks, due tasks, progress, streaks. | 4 | ✅ all passed |
| `KanbanOpsTest` | core/model | Board ordering rules: moving cards and columns, open/overdue cards. | 5 | ✅ all passed |
| `NicknameTest` | core/model | The nickname rule for names longer than 10 characters. | 2 | ✅ all passed |
| `PhysicsTest` | core/model | Gesture physics from Apple's fluid-interface talk: flick projection and the rubber band. | 3 | ✅ all passed |
| `PlannerTest` | core/model | Timetable and personal calendar: day view, next class, overlaps, merging events. | 3 | ✅ all passed |
| `PomodoroStatsTest` | core/model | Focus statistics: totals per day/week/subject, the streak, the countdown format. | 3 | ✅ all passed |
| `TodoAgendaTest` | core/model | Reminders/to-do grouping: overdue, today, upcoming, done; pending reminders. | 2 | ✅ all passed |

### Every test case

**AcademicCalendarTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `mascotLineMentionsUpcomingEventsAndStaysQuietOtherwise` | Penta warns about Diwali 2 days ahead and says nothing on an ordinary Thursday. | ✅ | 0.072 s |
| `stripCoversWholeSemester` | The day strip runs from 13 Aug (classes start) to 17 Dec (end of finals). | ✅ | 0.021 s |
| `multiDayEventsAndHolidays` | 2 Oct is a holiday; a 3-day assessment is found on its middle day and Penta says "day 2 of 3". | ✅ | 0.006 s |
| `bundledCalendarLoadsSemesterBounds` | The bundled CSV loads 31 events: classes 13 Aug – 25 Nov 2026, semester ends 17 Dec. | ✅ | 0.002 s |
| `weekendsAreHolidays` | Saturdays and Sundays are holidays; the semester has 70 class days. | ✅ | 0.002 s |

**BackupArchiveTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `entriesWithPathsAreIgnored` | Zip entries with paths ("../evil.csv", "dir/x.csv") are ignored (no zip slip). | ✅ | 0.006 s |
| `tooManyEntriesAreRejected` | A zip with 100 entries is rejected (zip-bomb guard). | ✅ | 0.003 s |
| `packAndUnpackKeepFilesAndManifest` | Files (text and binary) and the manifest survive pack → unpack unchanged. | ✅ | 0.004 s |

**CsvTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `roundTripsSpecialCharacters` | Commas, quotes, new lines and empty fields survive encode → parse. | ✅ | 0.002 s |
| `formulasAreNeutralisedOnExportAndRestoredOnImport` | Cells starting with = + - @ get a leading apostrophe on export and lose it on import. | ✅ | 0.000 s |
| `unreadableCopyKeepsTheOriginal` | An unreadable file is copied to `<name>.unreadable-<time>` unchanged; there is nothing to copy when the file is missing. | ✅ | 0.021 s |
| `interruptedWriteIsRecovered` | If a save was cut off after the old file was removed, the temp file is recovered. | ✅ | 0.004 s |
| `tableReadsByColumnName` | Rows are read by column name; a newer schema still reads older files. | ✅ | 0.004 s |

**CurriculumTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `topicsKeepBracketedListsTogether` | Topic splitting keeps "Lists (traversing, slices)" together and drops a leading "and". | ✅ | 0.004 s |
| `bundledCurriculumHasEverySemesterOneTheoryCourse` | CA711–CA719 load with 40 hours, units I–V and 3 outcomes each; catalog hours match. | ✅ | 0.016 s |

**PomodoroTimerTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `pauseKeepsRemainingAndShortResetIsNotRecorded` | Pausing keeps the remaining time; resetting after only 30 s records nothing (under the 60 s minimum). | ✅ | 0.200 s |
| `completedFocusIsRecordedAndMovesToBreak` | A finished 25-minute focus run is saved with its subject, and a break follows. | ✅ | 0.064 s |
| `longBreakAfterFourFocusSessions` | The fourth focus session is followed by a long break. | ✅ | 0.010 s |

**RepositoryTest** (`core/data`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `plannerSurvivesReloadAndMergesIntoTheCalendar` | Timetable slots and own events save, reload, validate kinds and merge into the calendar. | ✅ | 0.095 s |
| `everyTableChangeIsSeenByTheBackupWatcher` | The automatic backup hears about changes to grades, timetable, events, syllabus and the checklist (v0.32 fix). | ✅ | 0.053 s |
| `catalogLectureHoursFillOnlyEmptyValues` | Official 40 lecture hours fill only empty values, once; a manual "Clear" sticks. | ✅ | 0.036 s |
| `plainTodosMoveToTheBoardOnce` | Old to-dos without a time move to board cards once; reminders stay. | ✅ | 0.049 s |
| `dataSurvivesReload` | Subjects, attendance and settings (name, animation mode, theme, custom colours) reload from disk. | ✅ | 0.041 s |
| `gradesSurviveReloadAndImport` | Marks save, edit and reload; deleting a subject's marks works; grade CSVs import, bad rows dropped. | ✅ | 0.027 s |
| `habitsAndTicksSurviveReload` | Checklist tasks, weekdays and ticks reload; double ticks don't duplicate; delete removes history. | ✅ | 0.052 s |
| `kanbanBoardSurvivesReload` | Default columns are created once; cards move, keep notes/priority, and reload; deleting a column renumbers. | ✅ | 0.039 s |
| `timetableEventsAndSyllabusCountAsUserData` | A phone with only a timetable, an event or a syllabus tick is not "empty", so Google sign-in won't restore over it (v0.32 fix). | ✅ | 0.019 s |
| `addFromCatalogSkipsExistingCodes` | Adding the semester's subjects skips codes already added. | ✅ | 0.015 s |
| `clearingMarkRemovesRecord` | Re-marking replaces the day's mark; a double class (×2) is stored and reloads; clearing removes it. | ✅ | 0.015 s |
| `aFileThatCannotBeReadOrSavedNeitherCrashesNorLosesData` | An unreadable grades.csv is reported and kept; a failed save shows a message instead of crashing; other files still work (v0.32 fix). | ✅ | 0.027 s |
| `syllabusProgressSurvivesReload` | Covered syllabus units save (case-insensitive) and reload; unticking removes them. | ✅ | 0.018 s |
| `todosSurviveReload` | Reminders save with their time (seconds dropped), done flags reload, "clear done" works. | ✅ | 0.018 s |
| `csvImportMatchesTablesByHeaderAndReplaces` | CSV restore recognises files by columns (renamed, BOM, reordered), replaces only those tables, and a full restore empties missing ones. | ✅ | 0.060 s |

**AttendanceSummaryTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `doubleClassCountsAsTwo` | A ×2 mark counts as two classes; cancelled classes don't count; other subjects are ignored. | ✅ | 0.021 s |
| `emptySubjectIsOnTrack` | With no classes held a subject is on track at 0%. | ✅ | 0.000 s |
| `aboveTargetCanSkip` | Above target, the number of classes that can still be missed is right. | ✅ | 0.002 s |
| `exactlyOnTarget` | Exactly at the target counts as on track (no rounding error). | ✅ | 0.000 s |
| `lecturesLeftNeedsCourseHours` | Without lecture hours there is no "lectures left" figure. | ✅ | 0.000 s |
| `syllabusProgressCountsKnownUnitsOnly` | Syllabus progress counts only units of courses in the curriculum. | ✅ | 0.013 s |
| `lectureHoursGiveLecturesLeftAndSkips` | Lecture hours give lectures left, how many can be missed, and the best final %. | ✅ | 0.000 s |
| `belowTargetNeedsClasses` | Below target, the number of classes needed in a row is right. | ✅ | 0.000 s |
| `moreClassesThanHoursLeavesNoneLeft` | More classes marked than lecture hours leaves 0 left, never negative. | ✅ | 0.000 s |
| `unreachableTargetIsNegative` | When the target can no longer be reached, the outlook is negative. | ✅ | 0.000 s |

**CourseCatalogTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `shortNamesAndLabs` | Short names (DSA, DLCO, initials, "… Lab") and lab detection. | ✅ | 0.022 s |
| `missingIgnoresAlreadyAddedCodesCaseInsensitively` | With CA719 already added (any letter case), 6 catalog subjects are still missing. | ✅ | 0.000 s |
| `catalogHasSevenSubjectsWithUniqueCodes` | The catalog has 7 subjects with unique codes, 2 of them labs. | ✅ | 0.000 s |

**GradesTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `totalsAndLetters` | Subject totals, percentages, newest-first order and S–F letter grades. | ✅ | 0.006 s |
| `formatAndBounds` | Marks print as "18" / "13.5"; letter boundaries; zero maximum is safe. | ✅ | 0.001 s |

**HabitWeekTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `streakStopsAtTheStartDate` | Days before a checklist task existed don't break its streak. | ✅ | 0.002 s |
| `weekRunsMondayToSunday` | A week is Monday to Sunday. | ✅ | 0.002 s |
| `streakCountsScheduledDaysOnly` | Streaks count scheduled days only; an unticked today doesn't break it. | ✅ | 0.000 s |
| `dueAndProgressFollowTheWeekday` | Tasks due and "done of total" follow the weekday. | ✅ | 0.002 s |

**KanbanOpsTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `moveToAnotherColumnRenumbersBoth` | Moving a card to another column renumbers both columns. | ✅ | 0.005 s |
| `moveIntoEmptyColumn` | A card can move into an empty column; next position is computed. | ✅ | 0.001 s |
| `moveColumnRenumbers` | Moving a column renumbers all columns. | ✅ | 0.002 s |
| `reorderWithinColumnAndClampIndex` | Reordering within a column clamps out-of-range positions; unknown cards are a no-op. | ✅ | 0.000 s |
| `boardKnowsOpenAndOverdueCards` | Overdue is flagged except in the "done" column; due-soon order; next column. | ✅ | 0.004 s |

**NicknameTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `shortNamesAreKept` | Names of 10 characters or fewer are kept. | ✅ | 0.000 s |
| `longNamesBecomeFirstFourLetters` | Longer names become their first 4 letters ("Prabhanshu Kumar" → "Prab"). | ✅ | 0.000 s |

**PhysicsTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `projectionMatchesApplesFormula` | A 1000 px/s flick projects 499 px at 0.998 deceleration and 99 px at 0.99, matching Apple's formula. | ✅ | 0.001 s |
| `rubberBandResistsMoreTheFurtherItGoes` | Overscroll follows the finger less and less, never reaches the screen size, and is symmetric. | ✅ | 0.000 s |
| `rubberBandInverseUndoesIt` | Converting a shown rubber-band offset back gives the finger distance, so dragging back feels even. | ✅ | 0.000 s |

**PlannerTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `personalEventsMergeIntoTheCalendar` | Own events merge into the academic calendar once, marked personal. | ✅ | 0.005 s |
| `dayAndNext` | Classes for a day in time order, and the next / in-progress class. | ✅ | 0.002 s |
| `overlapsNeedSameDayAndTime` | Two classes overlap only on the same day with overlapping times. | ✅ | 0.000 s |

**PomodoroStatsTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `clockRoundsUpToTheWholeSecond` | The countdown reads 25:00 for a fresh session and rounds part-seconds up; never negative (v0.33). | ✅ | 0.002 s |
| `summarizesFocusTime` | Today (35), week (60) and total (110) focus minutes, completed sessions, streak and the 7-day chart. | ✅ | 0.016 s |
| `streakCountsFromYesterdayWhenTodayIsEmpty` | The focus streak counts from yesterday when nothing is done today yet. | ✅ | 0.001 s |

**TodoAgendaTest** (`core/model`)

| Test | Checks | Result | Time |
|---|---|---|---|
| `pendingRemindersAreFutureAndOpen` | Only future, not-done reminders are pending, soonest first. | ✅ | 0.003 s |
| `groupsByDayAndStatus` | To-dos group into overdue / today / upcoming / done in the right order. | ✅ | 0.002 s |

## Other checks attempted

| Check | How | Result |
|---|---|---|
| Release build | `./gradlew assembleRelease` (R8 minify, resource shrinking) after every feature, v0.1.0–v0.33.0 | Passed (APK 3.3 MB) |
| Android Lint | `./gradlew lintRelease` on all 16 Android modules (v0.31); re-run on `app`, `core:designsystem`, `core:social`, `core:backup`, `feature:friends`, `feature:tasks` for v0.32 | 0 errors. Fixed in v0.32: unused `R.color.green`, modifier parameter order, `mutableIntStateOf`. Left: portrait lock (intentional, ×2), `enableOnBackInvokedCallback` on old Android, `mipmap-anydpi-v26` (merging it breaks AGP 8.5, ISSUE-019), `CredentialDependency` (false positive: the dependency is in `core:social`), 22 library updates and AGP 9 available (a separate task) |
| APK inspection | `aapt dump badging`, `unzip -l` | Not debuggable; package `com.attendo.app`, versionCode 34, minSdk 26, targetSdk 35; no config files, keys or `DebugProbesKt.bin` inside |
| Release signature | `apksigner verify --print-certs` | Signed with the release key (`CN=Attendo`, SHA-1 `33:0C:B7:CA:…:D9:FA`), not the debug key |
| Compiler warnings | Every module recompiled from scratch (`--rerun-tasks`) | 0 warnings |
| Dead code | Script: declarations nothing references, properties never read, unused imports, unused module dependencies | Removed in v0.33 (3 properties, 66 imports, 3 dependencies); 20 duplicated control copies merged |
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
| v0.32 fixes | `everyTableChangeIsSeenByTheBackupWatcher` counted 0 changes | The test collected in `backgroundScope`, which `advanceUntilIdle()` doesn't run | Collected in a normal coroutine; the app code was right |
| v0.33 cleanup | `--rerun` recompiled only the last task, so the warning scan first saw nothing | Gradle applies `--rerun` to one task | Used `--rerun-tasks` |
| v0.33 cleanup | `AttendoTimePicker` call: "No value passed for parameter 'onPick'" | The trailing lambda bound to the last parameter (`confirmLabel`) | Made `onPick` the last parameter |
| v0.32 fixes | Build: `mipmap/ic_launcher not found` | Moving the adaptive icon to `mipmap-anydpi` (lint `ObsoleteSdkInt`) breaks resource linking on AGP 8.5 | Moved it back (ISSUE-019) |

## Not tested

- **No device or emulator runs.** Every screen, animation, theme, gesture (Tinder swipes, board drag and drop), the launch screen and the custom animation clock are checked only by compiling and Lint.
- **No UI or instrumented tests**, and no tests for the 16 modules outside `core:model` / `core:data` (ViewModels, `core:backup`, `core:social`, `app`).
- **Not exercised against real services:** Google sign-in, Google Drive backup/restore, Firebase Auth, Firestore reads/writes and the Firestore security rules (no emulator run), including v0.32's search limit, friend-code check and **Delete profile**.
- **Storage-problem toasts** are covered by unit tests only; how they look on a phone isn't checked.
- **v0.33 motion and materials** compile and pass lint, but the card swipe feel, the frosted-glass blur (and its cost on slow phones), the scroll-edge fade and the haptics have not been seen or felt on a device.
- **Not verified on hardware:** reminder notifications and exact alarms (Doze, OEM battery savers, reboot), notification permission prompts, the high-refresh-rate request.

## Code scan issues (2026-11-15) and where they stand

| # | Issue | Status |
|---|---|---|
| 1 | The automatic Google backup only watched some data tables | Fixed in v0.32.0; `everyTableChangeIsSeenByTheBackupWatcher` |
| 2 | Firestore rules let any signed-in student list every profile | Fixed in the rules (needs publishing in the Firebase console); not run against Firebase |
| 3 | A failed CSV write (e.g. storage full) crashed the app | Fixed; `aFileThatCannotBeReadOrSavedNeitherCrashesNorLosesData` |
| 4 | Sign-in treated a phone with only a timetable/events/syllabus as empty and restored over it | Fixed; `timetableEventsAndSyllabusCountAsUserData` |
| 5 | targetSdk 34; releases signed with the debug key | targetSdk 35 in v0.32.0; release key in v0.33.0 (docs/RELEASE_SIGNING.md) |
| — | An unreadable file was treated as empty and overwritten on the next save (found on re-check) | Fixed; `unreadableCopyKeepsTheOriginal` and the storage-failure test |
| — | Friend codes could be faked; no way to leave Friends (found on re-check) | Fixed in the app and the rules; not run against Firebase |
