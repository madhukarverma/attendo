# Security notes

Attendo is an offline-first app: all data lives in the app's private storage as CSV files, and the
only network traffic is the optional Google account backup. Review: 2026-11-11 (v0.27.0).

## What protects the data

| Area | Measure |
|---|---|
| Storage | CSV files in `filesDir/data`, private to the app (Android sandbox). Writes are atomic (temp file + rename); an interrupted write is recovered from the temp file on the next read. |
| Sharing | Exports go through a `FileProvider` limited to `cache/export/`, read-only grants, rebuilt on every export. |
| CSV injection | Exported cells starting with `=` `+` `-` `@` (or tab/CR) get a leading `'`, so Excel / Sheets show them as text instead of running formulas. CSV restore removes it again. |
| CSV restore | Files are matched by header, capped at 10 MB each, every row is decoded and validated (bad rows are skipped), and the user confirms before anything is replaced. |
| Backup archive | Flat entry names only (no paths, so no "zip slip"), max 64 entries, 20 MB per entry, 60 MB in total. Only known file names are used. |
| Network | HTTPS only: `usesCleartextTraffic="false"` and a network security config that trusts system CAs only (no user-installed CAs). Drive responses are capped at 64 MB. |
| Google sign-in | Play services `AuthorizationClient`, smallest Drive scope (`drive.appdata`: a hidden folder only Attendo can see). No client secret or ID in the APK; access tokens are kept in memory only, cleared on 401, revoked on sign-out. |
| Notifications | Reminder titles are hidden on the lock screen (private visibility with a generic public version). |
| Components | Only the launcher activity is exported. The reminder receiver is not exported; every `PendingIntent` is explicit and `FLAG_IMMUTABLE`. A notification tap can only open the Tasks tab. |
| Release build | R8 minification and resource shrinking; not debuggable. |

## Friends (Firebase)

| Area | Measure |
|---|---|
| Who sees what | Server-side Firestore rules (`firebase/firestore.rules`): profiles (name, programme, friend code) readable by signed-in students; friend lists and shared attendance/grades only by the owner and the owner's friends. |
| Friendships | Can only be created by the student accepting a pending request; either side can remove it. Requests can only be sent as yourself, never to yourself or an existing friend. |
| Sharing | Attendance sharing is a visible toggle; grades are off by default. Turning a toggle off deletes the server copy. Emails are never stored. |
| Validation | Rules allow only known fields with length limits (names ≤ 40 chars, ≤ 20 subjects per shared document). |
| Config | Firebase client settings are public by design and restricted to this package + SHA-1; `google-services.json` / `firebase.properties` are git-ignored. |

## Known limitations

- Release APKs are signed with the build PC's debug key (ISSUE-006). Anyone with that key could
  sign an update, so a private release keystore is needed before wider distribution.
- Data at rest is not encrypted beyond Android's file-based encryption, by design (the CSV files
  are meant to be readable when exported). Android Auto Backup is enabled, so the system may back
  the files up to the user's Google account.
- `USE_EXACT_ALARM` is fine for sideloading but restricted on Google Play (ISSUE-013).
- Nothing has been tested on a device yet.
