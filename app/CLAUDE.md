# app/

The `:app` module ties everything together. Kotlin sources are in `src/main/kotlin/com/attendo/app/`.

| File | What it is | Open it for |
|---|---|---|
| `AttendoApplication.kt` | `AppContainer` (manual DI: data, curriculum, Google backup, friends, reminders) and start-up migrations | adding a shared service, start-up work |
| `MainActivity.kt` | Theme, launch splash, opening a screen from a notification | start-up look, intents |
| `navigation/AttendoNavHost.kt` | `Routes` object, every screen's wiring, tab bar placement, slots passed between features | adding or wiring a screen, tabs, cross-feature links |
| `navigation/Transitions.kt` | Push, pop and sheet transitions for each Animations mode | how screens slide in and out |
| `reminders/ReminderScheduler.kt` | One exact alarm per pending reminder in `todos.csv` | when reminders fire |
| `reminders/ReminderReceiver.kt` | Receives the alarm and shows the notification | alarm handling |
| `reminders/ReminderNotifications.kt` | Notification channel and notification look | notification text, icon, tap action |
| `CsvExporter.kt` | Copies the CSV files to a cache folder and opens the share sheet | CSV export |
| `CsvImporter.kt` | Reads CSV files the user picked, for restore | CSV import from files |
| `LaunchImageStore.kt` | The launch image the user picks from their gallery | launch-screen image |
| `DisplayRefresh.kt` | Asks for the screen's highest refresh rate | smoothness on 90/120 Hz phones |

Other files:
- `build.gradle.kts`: version, release signing (from `../keystore.properties`, see
  `docs/RELEASE_SIGNING.md`), feature dependencies, Firebase settings (from
  `google-services.json`, else `../firebase.properties`).
- `src/main/AndroidManifest.xml`: permissions (notifications, exact alarms), activity, receiver,
  file provider.
- `src/main/res/`: launcher icon, notification icon, splash themes, `xml/file_paths.xml` (export
  sharing), `xml/network_security_config.xml`.

## Rules
- Every route lives in the `Routes` object; tab routes are listed in `Routes.TABS` in bar order.
- Feature modules don't know about each other. When one screen needs a piece of another, pass it
  in here as a composable slot or callback (examples: `syllabus` into the Focus tab,
  `syllabusSection` into the subject page).
- `google-services.json`, `firebase.properties`, `keystore.properties` and the key stay out of git.
