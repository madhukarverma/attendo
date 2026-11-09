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

## 2. App navigation (v0.19)

```mermaid
flowchart TD
    Launch([App launch]) --> Splash[Launch screen<br/>skipped in None mode]
    Splash --> Load{settings.csv<br/>onboarded?}
    Load -- no --> Onb[Onboarding<br/>name · nickname hint] --> Home
    Load -- yes --> Home
    Notif([Reminder notification]) --> Todo

    subgraph Tabs["Tab bar: Home · Classes · To-do · Timer · Stats"]
        Home[Home summary<br/>attendance · focus · today's to-dos<br/>reminders · exams]
        Classes[Classes<br/>day strip · Penta · mark subjects · Tinder mode]
        Todo[To-do & reminders<br/>overdue · today · upcoming · done]
        Timer[Pomodoro<br/>timer · stats · export]
        Stats[Statistics]
    end

    Home -- gear --> Settings[Settings<br/>profile · theme · animations · launch image<br/>Backup: Google account · CSV export / restore]
    Settings -- sign in --> Drive[(Google Drive<br/>app-data folder<br/>attendo-backup.zip)]
    Onb -- Restore from Google --> Drive
    Home -- exams card --> Cal[Academic calendar<br/>events + your to-dos]
    Classes -- calendar button --> Cal
    Cal -- pick a day --> Classes
    Classes -- Add a subject --> Add[Add subject<br/>name · code · target · lecture hours]
    Classes -- tap subject --> Detail[Subject detail<br/>% · tips · lectures left · history]
    Todo -- reminder time --> Alarm[(AlarmManager)] --> Notif
```

## 3. Architecture / data flow

```mermaid
flowchart LR
    subgraph app[":app"]
        Nav[NavHost + tab bar + AppContainer]
        Rem[reminders<br/>scheduler · receiver · notifications]
    end
    subgraph features["feature modules"]
        O[":feature:onboarding"]
        Db[":feature:dashboard (Home)"]
        At[":feature:attendance (Classes)"]
        T[":feature:todo"]
        P[":feature:pomodoro"]
        St[":feature:stats"]
        S[":feature:subjects"]
        C[":feature:calendar"]
        Se[":feature:settings"]
        Sp[":feature:splash"]
    end
    subgraph core["core modules"]
        DS[":core:designsystem<br/>theme · motion · mascots · tab bar"]
        D[":core:data<br/>repository interfaces<br/>CSV implementation · timer"]
        M[":core:model<br/>Subject · Attendance · Todo · Pomodoro<br/>calendar · nickname"]
    end
    Nav --> features
    features --> DS
    features --> D
    Rem --> D
    Bk[":core:backup<br/>Google sign-in · Drive app data"] --> D
    Se --> Bk
    O --> Bk
    Bk <--> G[("Google Drive<br/>app-data folder")]
    D --> M
    D <--> CSV[("filesDir/data/<br/>settings · subjects · attendance<br/>pomodoro · todos (.csv)")]
```

Screens follow **Route → ViewModel → StateFlow&lt;UiState&gt; → stateless Screen**. Features only see repository
interfaces, so the CSV storage can later be replaced (Room, cloud sync) without UI changes.
