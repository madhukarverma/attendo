# Attendo — Flowcharts

GitHub renders these Mermaid diagrams directly.

## 1. Delivery plan (one task = one branch = one push)

```mermaid
flowchart TD
    A[Task 0: Flowchart + context docs] --> B[Task 1: Base framework<br/>multi-module Gradle, theme, mascots,<br/>CSV data layer, onboarding, home shell]
    B --> B2[Build base APK]
    B2 --> P0[(push → master)]
    P0 --> F1[Task 2: feature/subjects<br/>add + list + delete subjects]
    F1 --> P1[(push branch, merge → master)]
    P1 --> F2[Task 3: feature/attendance<br/>day strip + mark Present/Absent/Cancelled<br/>live percentages]
    F2 --> P2[(push branch, merge → master)]
    P2 --> F3[Task 4: feature/subject-detail<br/>percentage, bunk calculator tips, history]
    F3 --> P3[(push branch, merge → master)]
    P3 --> F4[Task 5: feature/stats<br/>overall + per-subject bars]
    F4 --> P4[(push branch, merge → master)]
    P4 --> F5[Task 6: feature/settings<br/>name, export CSV files]
    F5 --> P5[(push branch, merge → master)]
    P5 --> R[Build final APK v0.1]
    R --> N[Future: timetable, reminders,<br/>multiple lectures/day, widgets, sync]
```

## 2. App navigation

```mermaid
flowchart TD
    Launch([App launch]) --> Load{settings.csv<br/>onboarded?}
    Load -- no --> Onb[Onboarding<br/>enter name]
    Onb --> Home
    Load -- yes --> Home[Home<br/>greeting · day strip · mascot tip<br/>subjects for selected day]
    Home -- tap Present / Absent / Cancel --> Mark[Write attendance.csv] --> Home
    Home -- + New subject --> Add[Add subject<br/>name · code · target %] --> Home
    Home -- tap subject --> Detail[Subject detail<br/>% · can skip / must attend · history]
    Detail -- delete --> Home
    Home -- stats icon --> Stats[Statistics]
    Home -- settings icon --> Settings[Settings<br/>name · export CSV]
```

## 3. Architecture / data flow

```mermaid
flowchart LR
    subgraph app[":app"]
        Nav[NavHost + AppContainer]
    end
    subgraph features["feature modules"]
        O[":feature:onboarding"]
        H[":feature:home"]
        S[":feature:subjects"]
        St[":feature:stats"]
        Se[":feature:settings"]
    end
    subgraph core["core modules"]
        DS[":core:designsystem<br/>theme · mascots · components"]
        D[":core:data<br/>repository interfaces<br/>CSV implementation"]
        M[":core:model<br/>Subject · AttendanceRecord<br/>AttendanceSummary maths"]
    end
    Nav --> features
    features --> DS
    features --> D
    D --> M
    D <--> CSV[("filesDir/data/<br/>subjects.csv<br/>attendance.csv<br/>settings.csv")]
```

Screens follow **Route → ViewModel → StateFlow&lt;UiState&gt; → stateless Screen**. Features only see repository
interfaces, so the CSV storage can later be replaced (Room, cloud sync) without UI changes.
