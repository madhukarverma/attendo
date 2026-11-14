# Release signing

Since v0.33.0, release APKs are signed with Attendo's own release key instead of the build PC's
debug key. Android only installs an update over an app signed with the same key, so this key is
what every future version must be signed with.

## Where things are

| What | Where | In git? |
|---|---|---|
| The key | `%USERPROFILE%\android-dev\keys\attendo-release.jks` (alias `attendo`, RSA 4096, valid ~27 years) | No (`*.jks` is ignored) |
| Path and password for the build | `keystore.properties` in the repository root | No (ignored) |
| Template | `keystore.properties.example` | Yes |
| Build setup | `releaseKey` and `signingConfigs` in `app/build.gradle.kts` | Yes |

Without `keystore.properties`, release builds fall back to the debug key and Gradle prints a
warning. Don't publish such a build: it can't update a release-signed install.

## Creating the key (one time)

The key was created with a script that asks for the password and writes `keystore.properties`.
To do the same by hand (JDK 17's `keytool`):

```bash
keytool -genkeypair -keystore attendo-release.jks -storetype PKCS12 -alias attendo -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Attendo"
```

Then copy `keystore.properties.example` to `keystore.properties` and fill in the path and password.

## After creating it

1. **Back up** the `.jks` file and the password (a password manager, plus a copy of the file
   somewhere other than this PC). If either is lost, no update can ever be installed over the app.
2. **Firebase:** add the key's SHA-1 (and SHA-256) under Project settings › Your apps › Attendo ›
   Add fingerprint. Google sign-in (Friends and the Drive backup) checks it. Keep the debug key's
   fingerprint too, for debug builds. To print the fingerprints:

   ```bash
   keytool -list -v -keystore attendo-release.jks -alias attendo
   ```

## Moving a phone to the release-signed app (one time)

A phone with a debug-signed Attendo (v0.32.0 or older) can't update to a release-signed one:

1. Back up: Settings › Backup › **Back up now** (Google account), or **Export CSV**.
2. Uninstall Attendo.
3. Install the new APK and sign in to Google (or import the CSV files) to restore.
