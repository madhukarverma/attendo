# docs/

Guides and project memory. `context/` follows the user's AI context framework.

| File | What it answers | Open it when |
|---|---|---|
| `context/MASTER_CONTEXT.md` | Long-lived facts: goal, technology, constraints, design tokens, repository | you need the big picture |
| `context/CURRENT_STATE.md` | What is built now and what is in progress | starting work, after a feature |
| `context/CHANGELOG.md` | What changed in each version, newest first | writing release notes, "when did X change?" |
| `context/DECISIONS.md` | Why things are the way they are (`DEC-001`…) | before changing a design choice |
| `context/OPEN_ISSUES.md` | Known gaps and requests still to do (`ISSUE-001`…) | picking the next task, closing an issue |
| `FLOWCHART.md` | Mermaid diagrams: delivery plan, navigation, architecture | screens or modules change |
| `ERROR_CODES.md` | Every error code, its message and what to do | a code is reported, adding an error |
| `FEEDBACK.md` | Where feedback goes and how to read it in the Firebase console | reading feedback |
| `FRIENDS_SETUP.md` | One-time Firebase setup for friends and the Google backup | setting up or debugging sign-in |
| `GOOGLE_BACKUP_SETUP.md` | How the Drive backup works (setup now lives in `FRIENDS_SETUP.md`) | backup questions |
| `RELEASE_SIGNING.md` | Where the release key is, backing it up, Firebase fingerprints, moving a phone to the release-signed app | releasing, signing errors, a new PC |
| `SECURITY.md` | What protects the data, Firestore privacy, the last audit, known risks | security or privacy changes |
| `TEST_REPORT.md` | Every unit test, last results, what isn't tested | test questions; refresh after test changes |
| `images/` | README banner and charts (light and dark SVGs), drawn by `make_images.py` | changing the README pictures (edit the script, run it, commit the SVGs) |

## Rules
- After every feature, update in the same branch: `CHANGELOG.md` (new version on top),
  `CURRENT_STATE.md`, and `OPEN_ISSUES.md` (strike through anything finished, with its version).
- Add a `DECISIONS.md` entry only when a real choice was made; never rewrite old entries, change
  their `Status` line instead.
- Update `FLOWCHART.md` when screens, navigation or modules change.
- Requests the user wants kept on the list ("needs to be done") go at the end of `OPEN_ISSUES.md`.
