# core/designsystem/

Look and feel shared by every screen. Sources are in
`src/main/kotlin/com/attendo/core/designsystem/`.

| File | What it is | Open it for |
|---|---|---|
| `theme/Tokens.kt` | `Space` (the spacing scale) | any padding or gap |
| `theme/ContinuousShape.kt` | Apple's continuous corner (`ContinuousShape(radius)`), used for every rounded box | corners |
| `theme/Fonts.kt` | Settings › Font: the bundled fonts (`res/font`, licences in `fonts/`) and `withFont` | adding or changing a font |
| `theme/Theme.kt` | Theme fonts, colour palettes for each theme, `AttendoColors`, `AttendoTheme`, contrast helpers | colours, adding a theme |
| `motion/Motion.kt` | Springs for each Animations mode (`push`, `sheet`, `settle`, `snappy`, `release`), `LocalAnimationMode` | how fast or bouncy things move |
| `motion/AppMotionScale.kt` | App-wide animation speed, follows the system animator scale | global animation speed |
| `motion/IosPress.kt` | `Modifier.iosPress`: the press-and-shrink effect | tap feedback |
| `motion/BounceScroll.kt` | `BounceContainer`: rubber-band overscroll (Apple's formula) | scroll bounce |
| `motion/Haptics.kt` | `rememberHaptics()`: `tick`, `confirm`, `grab` | vibration feedback |
| `motion/LargeTitle.kt` | `PageHeader` (the title row every tab shares, so titles line up), large titles that collapse into a top bar with a scroll-edge fade | screen headers |
| `component/Components.kt` | Cards, buttons, speech bubble, squiggle, animated numbers, small glyphs | shared building blocks |
| `component/Glass.kt` | Frosted glass: `rememberBackdrop`, `backdropSource`, `glass` (blur on Android 12+) | translucent surfaces |
| `component/Controls.kt` | `PillButton`, `NavigationRow` (plain row that opens a screen), `HeroStat`, `ProgressBar` (animated in the draw phase), text-field colours, date and time pickers | buttons, rows, bars, forms, pickers |
| `component/AttendoIcons.kt` | Line icons for marks Material doesn't have (check, cross, no class, undo, feedback, alert) | icons instead of symbols |
| `component/EmptyState.kt` | `EmptyState` (icon, title, message, action) and `ErrorNote` (an error with its code) | empty lists, inline errors |
| `component/TabBar.kt` | The bottom tab bar and `TabBarClearance` | tab bar look |
| `component/SegmentedControl.kt` | Segmented switcher (the Focus and Tasks tab pages) | page switchers |
| `component/DayStrip.kt` | Horizontal strip of days | picking a day |
| `component/Mascots.kt` | The DNA mascots (pentagon on Home, circle on Classes, rectangle on Tasks), the onboarding blob, mood faces and the shared idle animation | mascots |
| `component/Confetti.kt` | Confetti burst | celebrations |
| `component/LaunchArt.kt` | Launch-screen background, batch label, "developed by" line | launch screen |

## Rules
- Spacing comes from `Space` only (4, 8, 12, 16, 24, 32, 48 dp) (DEC-024).
- Rounded boxes use `ContinuousShape(radius)` with the radius that suits that box; there is no
  single app-wide radius. Nested boxes: inner radius = outer radius − padding. Buttons, chips and
  the tab bar are capsules, round things circles (`CircleShape`) (DEC-026, DEC-027).
- Every tab starts with `PageHeader`, so titles sit at the same height on all five.
- The default font is the phone's own (apple-design skill: system font first); bundled fonts
  are only used when chosen in Settings › Font.
- Icons, not text symbols or emoji: Material `Icons.Filled` or `AttendoIcons`. Empty lists use
  `EmptyState`; errors show their `ErrorCode` (docs/ERROR_CODES.md).
- Take colours from `AttendoColors` so every theme works. Don't add new raw `Color(0x…)` values
  in screens.
- Animations use the specs in `Motion` and must respect the user's Animations setting: with
  `LocalAnimationMode.current == AnimationMode.NONE` nothing moves.
- Springs are critically damped by default (`snappy`, `settle`). Use `release` (a little bounce)
  only after a drag or flick, started with the finger's velocity; decide flicks with
  `Physics.project`. Animate from the current on-screen value so motion can be interrupted.
- Haptics only through `rememberHaptics()`, and only for moments that matter (DEC-021).
- These motion rules come from `.claude/skills/apple-design/SKILL.md` (written for the web: CSS,
  Pointer Events). Use the helpers here as its Compose version.
- Moving values are read only in draw or layout lambdas (`graphicsLayer {}`, `drawBehind {}`,
  `Canvas`), never in a composable body, and nothing animates its size to show progress: use
  `ProgressBar` (`.claude/skills/animation-performance/SKILL.md`, DEC-022).
- Tab screens pad their content by `TabBarClearance` at the bottom so the tab bar doesn't cover it.
- When a second feature needs the same control, move it here instead of copying it.
