# Google account backup — one-time setup

> Since v0.30 the backup and the friends feature share one Firebase project. Follow
> [FRIENDS_SETUP.md](FRIENDS_SETUP.md), which covers both; this page explains the backup itself.

Settings › Backup › **Sign in with Google** stores all of Attendo's data in the user's own Google
Drive, in the hidden *app data* folder (scope `https://www.googleapis.com/auth/drive.appdata`).
Only Attendo can read that folder; it doesn't show up in the Drive file list.

Google only lets an app use that scope once the app is registered in a Google Cloud project. No
client ID or secret is compiled into the APK: Google Play services recognises the app by its
**package name** and **signing certificate (SHA-1)**. Until this is done, sign-in shows
"Google sign-in isn't set up for this build yet".

## Values for this app

| Field | Value |
|---|---|
| Package name | `com.attendo.app` |
| SHA-1 (debug key on the build PC, used for every release so far) | `3F:82:53:4D:8D:3D:B6:C5:FB:A5:35:A4:26:1D:0E:43:33:57:48:28` |

The SHA-1 belongs to `C:\Users\hp\.android\debug.keystore`. Keep a copy of that file: an APK built
with a different key needs its own SHA-1 added to the OAuth client (`./gradlew signingReport`
prints it).

## Steps (Google Cloud console)

1. Open https://console.cloud.google.com/ and create a project (e.g. "Attendo").
2. **APIs & Services › Library** → search "Google Drive API" → **Enable**.
3. **Google Auth Platform** (OAuth consent screen):
   - Branding: app name "Attendo", your support email.
   - Audience: **External**, publishing status **Testing**, and add every Google account that should
     be able to sign in under **Test users**.
   - Data access: **Add or remove scopes** → add `.../auth/drive.appdata`.
4. **Clients** (or APIs & Services › Credentials) → **Create client** → type **Android** →
   package name and SHA-1 from the table above → Create.
5. Wait a few minutes, install the APK, then Settings › Backup › Sign in with Google.

## How it behaves

- Signing in with no backup in the account uploads this phone's data.
- Signing in on a phone with no data (fresh install, or "Restore from Google" on the welcome
  screen) restores the backup straight away.
- If both have data, the app asks: restore the backup, or keep this phone's data and replace it.
- While signed in, the backup is updated a few seconds after every change (Settings switch
  "Back up automatically"), and on demand with **Back up now**.
- **Sign out** forgets the account on this phone and revokes the app's Drive access; the backup
  stays in the account. To delete it: drive.google.com › Settings › Manage apps › Attendo ›
  Options › Delete hidden app data.
