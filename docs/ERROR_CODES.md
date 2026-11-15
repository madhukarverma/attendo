# Error codes

Every problem Attendo reports ends with a code, for example "No internet connection. (FR-404)".
The codes are defined in `core/model/src/main/kotlin/com/attendo/core/model/ErrorCode.kt`; a code
never changes meaning, and a retired code is never reused. `%s` in a message is filled in with a
file name or detail. Cancelling a sign-in isn't an error, so it has no code.

When you add or change a code, add its row here in the same change.

## Storage (ST)

| Code | Message | What to do |
|---|---|---|
| `ST-101` | %s couldn't be read, so it starts empty. The old file was kept as %s. | Open the kept copy in a spreadsheet to recover anything you need, then re-enter it. |
| `ST-102` | %s couldn't be read, so it starts empty. | Restore from Google backup or a CSV export if you have one. |
| `ST-103` | %s couldn't be read. Changes to it won't be saved until the app is restarted. | Restart the app. If it keeps happening, restore from a backup. |
| `ST-104` | Changes to %s can't be saved because the file couldn't be read. Restart the app. | Restart the app. |
| `ST-105` | Couldn't save your change to %s. Your phone may be out of storage space. | Free up storage space on the phone, then try again. |

## CSV restore (IM)

| Code | Message | What to do |
|---|---|---|
| `IM-201` | Couldn't read those files. Pick CSV files exported from Attendo. | Pick the CSV files Attendo exported (Settings › Backup › Export). |
| `IM-202` | Restore failed: %s | Try again; the detail after "Restore failed:" says what went wrong. |
| `IM-203` | Those files don't look like Attendo CSV exports. | Pick files exported by Attendo; other CSV files aren't recognised. |

## Google backup (BK)

| Code | Message | What to do |
|---|---|---|
| `BK-301` | Google needs you to sign in again. Tap Sign in with Google. | Tap Sign in with Google in Settings › Backup. |
| `BK-302` | Google sign-in isn't set up for this build of the app. | The build has no Google OAuth client for its key: add the release SHA-1 in Firebase (docs/RELEASE_SIGNING.md). |
| `BK-303` | No internet connection. Backup will retry after your next change. | Nothing to do; the backup runs again after your next change once you're online. |
| `BK-304` | Google Drive refused access. The app's Google project may need the Drive API turned on. | Turn on the Google Drive API for the Firebase project (docs/FRIENDS_SETUP.md, step 5). |
| `BK-399` | Something went wrong with the backup: %s | Try again. If it repeats, send feedback with the code and detail. |

## Friends (FR)

| Code | Message | What to do |
|---|---|---|
| `FR-401` | Friends isn't set up in this build of the app. | The build has no Firebase project (docs/FRIENDS_SETUP.md). |
| `FR-402` | No Google account found on this phone. Add one in system settings, then try again. | Add a Google account in the phone's settings. |
| `FR-403` | Google sign-in failed: %s | Try again. Check that the release SHA-1 is added in Firebase. |
| `FR-404` | No internet connection. | Connect to the internet and try again. |
| `FR-405` | The Friends server refused this. Its privacy rules may need updating. | Publish the latest firebase/firestore.rules in the Firebase console. |
| `FR-406` | The Friends server can't be reached. Check your connection. | Check the connection and try again later. |
| `FR-407` | This student isn't on Attendo any more. | Nothing to do; they deleted their Friends profile. |
| `FR-408` | Sign in to Friends first. | Sign in on the Friends screen first. |
| `FR-499` | Friends server error: %s | Try again. If it repeats, send feedback with the code. |

## Feedback (FB)

| Code | Message | What to do |
|---|---|---|
| `FB-501` | Feedback can't be sent from this build of the app. | The build has no Firebase project (docs/FRIENDS_SETUP.md). |
| `FB-502` | You just sent feedback. Try again in a minute. | Wait a minute before sending more feedback. |
| `FB-503` | Add a rating, or a message of at least 10 characters. | Tap a star, or write at least 10 characters. |
| `FB-504` | That email address doesn't look right. | Fix the email address, or leave it empty. |
| `FB-505` | The server refused the feedback. Its rules may need updating. | Publish the latest firebase/firestore.rules in the Firebase console. |
| `FB-599` | Couldn't send feedback: %s | Try again; the detail says what went wrong. |
