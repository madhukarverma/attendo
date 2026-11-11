# Friends + Google backup — one-time setup (free)

Both online features use one free Firebase project (Spark plan, no card):

- **Friends** — Firebase Authentication (Sign in with Google) and Cloud Firestore.
- **Google account backup** — Google Drive's hidden app-data folder, authorised in the same
  project (an Android OAuth client for a package + SHA-1 can only exist in one project, so both
  features must share it).

Until this is done the app works normally; Friends shows "Not set up yet" and backup sign-in
shows "isn't set up for this build yet".

| Value | |
|---|---|
| Package name | `com.attendo.app` |
| SHA-1 (debug key on the build PC) | `3F:82:53:4D:8D:3D:B6:C5:FB:A5:35:A4:26:1D:0E:43:33:57:48:28` |

Google Cloud requires 2-Step Verification on the Google account you use for this.

## 1. Create the Firebase project

1. Open https://console.firebase.google.com and click **Create a project**. If you already made an
   "Attendo" project in the Google Cloud console, pick it from the list instead (Firebase is added
   to it).
2. Google Analytics: not needed — switch it off. Create.

## 2. Add the Android app

1. On the project overview, click the **Android** icon (Add app).
2. Package name `com.attendo.app`, nickname `Attendo`, and the **SHA-1** above. Register app.
3. Download **google-services.json** (keep it; step 6). Skip the remaining SDK steps — the
   project is already set up for it.

## 3. Turn on Google sign-in

**Build › Authentication › Get started › Sign-in method › Google › Enable**, choose your support
email, **Save**.

## 4. Create the database and its rules

1. **Build › Firestore Database › Create database**.
2. Location: **asia-south1 (Mumbai)** (closest to Trichy; can't be changed later).
3. Start in **production mode** › Create.
4. Open the **Rules** tab, replace everything with the contents of
   [`firebase/firestore.rules`](../firebase/firestore.rules), and click **Publish**. These rules
   are what keep attendance visible to friends only and grades private.

## 5. Google backup (Drive) in the same project

1. Open https://console.cloud.google.com, select the **same project**.
2. **APIs & Services › Library › Google Drive API › Enable**.
3. **Google Auth Platform › Data access › Add or remove scopes**, add
   `https://www.googleapis.com/auth/drive.appdata`, Update, Save.
4. **Audience**: while the app is in **Testing**, only listed **test users** can sign in (add your
   Gmail and your friends'). To let any classmate sign in, click **Publish app** (basic sign-in
   needs no review; with the Drive scope, Google may show an "unverified app" warning until the app
   is verified).
5. Don't create another Android OAuth client: step 2 already made one for this package and SHA-1.

## 6. Build it in

Put the downloaded `google-services.json` in the repository at `Attendo/app/google-services.json`
(re-download it after step 3 so it contains the web client). It's git-ignored. Rebuild, and the
release APK has Friends and backup sign-in enabled. Alternatively fill `firebase.properties` from
`firebase.properties.example`.

## How Friends works

- Sign in from **Home › person icon** (or Settings › Friends). Your profile shows your name, the
  programme and a 6-character **friend code**.
- Search classmates by the start of their name or by friend code; **Add** sends a request, which
  they **Accept** or **Decline**.
- Friends see each other's friend lists, and each other's attendance while **Attendance** sharing
  is on. **Grades** are shared only if you turn that switch on (off by default).
- Any student can see your name, programme and friend code; nothing else is visible to people who
  aren't your friends. Turning a switch off deletes the shared copy from the server.

Firebase's free tier (50,000 reads and 20,000 writes a day) is far more than a class needs.
