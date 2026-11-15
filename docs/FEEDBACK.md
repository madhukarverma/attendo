# Feedback: where it goes and how to read it

Students send feedback from **Settings › Support › Send feedback**: a 1–5 star rating, what it's
about (Problem, Idea, Praise, Other), a message, an optional email for a reply, and, if they leave
the switch on, the app version, Android version and phone model.

## Reading it

1. Open https://console.firebase.google.com and pick the Attendo project.
2. **Build › Firestore Database › Data**, then the **`feedback`** collection.
3. Each document is one piece of feedback. Click **createdAt** to sort by time.

| Field | What it is |
|---|---|
| `rating` | 1–5 stars, or 0 if they didn't rate |
| `category` | `bug`, `idea`, `praise` or `other` |
| `message` | What they wrote (up to 2,000 characters; can be empty when they only rated) |
| `contact` | Their email, only if they typed one and want a reply |
| `app`, `versionCode` | Attendo version, e.g. `0.35.0` / `37` |
| `android` | Android SDK level, e.g. `35` (Android 15) |
| `device` | Phone maker and model |
| `createdAt` | When it arrived (server time) |

The **reference** a student sees after sending (e.g. `7F3K9QAB`) is the first 8 characters of the
document ID, in capitals. Search for it to find their message.

## Before it works

The `feedback` rules have to be live: paste `firebase/firestore.rules` into **Firestore › Rules**
and **Publish** (needed once after v0.35.0). Until then sending fails with **FB-505**.

## How it's protected

- Anyone with the app can send feedback without signing in, but nobody can read, change or delete
  it through the app, not even the sender. Only the project owner sees it, in the console.
- The rules accept only the fields above, with size limits, and the server's own timestamp.
- The app sends at most one piece of feedback a minute. Offline, Firestore keeps it on the phone and
  sends it later (the student sees "Saved. It'll send when you're online.").
- Nothing from the student's attendance, marks or tasks is ever included.
- The rules are tested in `firebase/test/rules.test.mjs` (the `feedback` group).

Limit: because no sign-in is needed, someone who extracts the app's settings could write junk
feedback with a script, using up the free daily write quota. Delete junk documents in the console;
if it becomes a problem, require sign-in for feedback in the rules.
