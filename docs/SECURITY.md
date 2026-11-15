# Security notes

Attendo is an offline-first app: all data lives in the app's private storage as CSV files, and the
only network traffic is the optional Google account backup and the optional Friends feature. There
is no server of Attendo's own: Friends uses Firebase (Google's servers) guarded by
`firebase/firestore.rules`, and the backup uses the user's own Google Drive.

Last full audit: 2026-11-15 (v0.34.0). Earlier reviews: v0.27.0, v0.32.0.

## What protects the data

| Area | Measure |
|---|---|
| Storage | CSV files in `filesDir/data`, private to the app (Android sandbox). Writes are atomic (temp file + rename); an interrupted write is recovered from the temp file on the next read. |
| Sharing | Exports go through a `FileProvider` limited to `cache/export/`, read-only grants, rebuilt on every export. |
| CSV injection | Exported cells starting with `=` `+` `-` `@` (or tab/CR) get a leading `'`, so Excel / Sheets show them as text instead of running formulas. CSV restore removes it again. |
| CSV restore | Files are matched by header, capped at 10 MB each, every row is decoded and validated (bad rows are skipped), and the user confirms before anything is replaced. |
| Backup archive | Flat entry names only (no paths, so no "zip slip"), max 64 entries, 20 MB per entry, 60 MB in total. Only known file names are used. |
| Android backup | Android's own cloud backup and phone-to-phone transfer include the CSV data but leave out the libraries' sign-in state and Firestore's cache of friends' data (`data_extraction_rules.xml`, `backup_rules.xml`). |
| Network | HTTPS only: `usesCleartextTraffic="false"` and a network security config that trusts system CAs only (no user-installed CAs). Drive responses are capped at 64 MB. |
| Google sign-in | Play services `AuthorizationClient`, smallest Drive scope (`drive.appdata`: a hidden folder only Attendo can see). No client secret in the APK; access tokens are kept in memory only, cleared on 401, revoked on sign-out. |
| Notifications | Reminder titles are hidden on the lock screen (private visibility with a generic public version). |
| Components | Attendo exports only its launcher activity, which accepts nothing but the `attendo://todo` link (it can only open the Tasks tab). The reminder receiver isn't exported; every `PendingIntent` is explicit and `FLAG_IMMUTABLE`. The other exported entries in the merged manifest come from Firebase Auth and Play services, and are either sign-in callbacks or guarded by system permissions. |
| Input | Compose shows all text as plain text (no WebView, no HTML), so there is nothing for cross-site scripting to hit. Names sent to Firebase are capped at 40 characters; search waits 350 ms after typing and asks for 20 results at most. |
| Release build | R8 minification and resource shrinking; not debuggable; signed with the release key (`docs/RELEASE_SIGNING.md`). No logging calls in the code. |
| Build | The Gradle download is checked against its published SHA-256 (`distributionSha256Sum`); the wrapper jar matches Gradle's published checksum. |

## Friends (Firebase)

| Area | Measure |
|---|---|
| Who counts as signed in | Only Google accounts. Anonymous or email/password accounts are refused by the rules, even if those sign-in methods were switched on in the console. |
| Who sees what | Profiles (name, programme, friend code) are readable by signed-in students, one at a time or 20 per search; friend lists and shared attendance/grades only by the owner and the owner's friends. |
| Friend codes | Always derived from the account id. The rules reject a profile or request that claims another account's code, and the app ignores codes stored in documents. |
| Leaving | **Delete profile** removes the profile, both sides of every friendship, pending requests and shared data, then signs out. |
| Friendships | Can only be created by the student accepting a pending request; either side can remove it. Requests can only be sent as yourself, never to yourself or an existing friend, and only once per pair while one is pending. |
| Sharing | Attendance sharing is a visible toggle; grades are off by default. Turning a toggle off deletes the server copy. Emails are never stored. |
| Validation | Only known fields, with length limits (names ≤ 40 characters, ≤ 20 subjects per shared document). Every timestamp must be the server's own time, so no field can be filled with junk. |
| Tested | `firebase/test/rules.test.mjs` runs 32 checks on the local emulator: every read and write the app makes must be allowed, and every attack in the list must be refused. Run it before publishing any rules change. |
| Checked live | 2026-11-15: reading or writing the real database without signing in is refused (HTTP 403). |
| Config | The Firebase API key, project ID and app ID are compiled into the APK. They identify the project and are public by design (every Firebase app ships them); the rules are what protect the data. `google-services.json` / `firebase.properties` are git-ignored anyway. |

## Checked and clean (2026-11-15)

- **Secrets in git:** all 93 commits on every branch searched for API keys, tokens, private keys,
  passwords and key files (`.jks`, `google-services.json`, `keystore.properties`): none.
- **Secrets in the APK:** the only key-like string is the Firebase API key (see Config above).
- **Libraries:** all 168 libraries in the release build checked against the OSV vulnerability
  database. One hit, Guava 32.1.3 (CVE-2026-102554, Java deserialization), which Attendo can't
  trigger because it never deserializes Java objects; Guava is pinned to the fixed 33.8.0 anyway.
- **Not applicable to an app without its own server:** admin routes, CORS, HTTP security headers,
  server rate limiting, password hashing (Attendo stores no passwords; Google handles sign-in).

## Known limitations

- **The GitHub repository is public.** Code, docs and APKs can be read by anyone. Nothing secret
  is in it, but it shows the programme and college. Earlier commits still contain the developer's
  name as an example in the nickname code.
- **Profiles can be listed page by page.** Each search returns at most 20 profiles, but a signed-in
  student who keeps paging can still collect every profile's name, programme and friend code.
  Nothing else is visible to non-friends.
- **No server-side rate limit.** Firestore on the free plan can't count requests per user, so a
  student who writes a script could use up the project's free daily quota (50,000 reads, 20,000
  writes). The free plan can't run up a bill. A real limit would need Firebase App Check (which
  needs Play Store distribution) or Cloud Functions (paid plan).
- **The Firebase API key has no app restriction** (2026-11-15: it answers calls from any
  computer, not only from the app). It can't be hidden in an app either way. Firebase limits its
  auto-created keys to Firebase APIs, which is the part that matters; check that this is still so
  (`docs/FRIENDS_SETUP.md`, step 7).
- Releases are signed with a private key kept outside the repository (`docs/RELEASE_SIGNING.md`).
  Losing the key or its password means no further updates can be installed over the app, so both
  must be backed up. The password sits in plain text in the git-ignored `keystore.properties`.
- Data at rest is not encrypted beyond Android's file-based encryption, by design (the CSV files
  are meant to be readable when exported).
- The build tools are old (Android Gradle plugin 8.5.2, Kotlin 2.0.20), so the newest Firebase and
  Google sign-in libraries can't be used yet (ISSUE-023).
- `USE_EXACT_ALARM` is fine for sideloading but restricted on Google Play (ISSUE-013).
- Nothing has been tested on a device yet.
