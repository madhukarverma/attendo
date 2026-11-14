# core/designsystem/

Look and feel shared by every screen. Sources are in
`src/main/kotlin/com/attendo/core/designsystem/`.

| File | What it is | Open it for |
|---|---|---|
| `theme/Theme.kt` | Fonts, colour palettes for each theme, `AttendoColors`, `AttendoTheme`, contrast helpers | colours, fonts, adding a theme |
| `motion/Motion.kt` | Springs for each Animations mode (`push`, `sheet`, `settle`, `snappy`, `release`), `LocalAnimationMode` | how fast or bouncy things move |
| `motion/AppMotionScale.kt` | App-wide animation speed, follows the system animator scale | global animation speed |
| `motion/IosPress.kt` | `Modifier.iosPress`: the press-and-shrink effect | tap feedback |
| `motion/BounceScroll.kt` | `BounceContainer`: rubber-band overscroll (Apple's formula) | scroll bounce |
| `motion/Haptics.kt` | `rememberHaptics()`: `tick`, `confirm`, `grab` | vibration feedback |
| `motion/LargeTitle.kt` | Large titles that collapse into a top bar with a scroll-edge fade | screen headers |
| `component/Components.kt` | Cards, buttons, speech bubble, squiggle, animated numbers, small glyphs | shared building blocks |
| `component/Glass.kt` | Frosted glass: `rememberBackdrop`, `backdropSource`, `glass` (blur on Android 12+) | translucent surfaces |
| `component/Controls.kt` | `PillButton`, `HeroStat`, text-field colours, date and time pickers | buttons, forms, pickers |
| `component/TabBar.kt` | The bottom tab bar and `TabBarClearance` | tab bar look |
| `component/SegmentedControl.kt` | Segmented switcher (the Focus and Tasks tab pages) | page switchers |
| `component/DayStrip.kt` | Horizontal strip of days | picking a day |
| `component/Mascots.kt` | Mascot shapes and mood faces | mascots |
| `component/Confetti.kt` | Confetti burst | celebrations |
| `component/LaunchArt.kt` | Launch-screen background, batch label, "developed by" line | launch screen |

## Rules
- Take colours from `AttendoColors` so every theme works. Don't add new raw `Color(0x…)` values
  in screens.
- Animations use the specs in `Motion` and must respect the user's Animations setting: with
  `LocalAnimationMode.current == AnimationMode.NONE` nothing moves.
- Springs are critically damped by default (`snappy`, `settle`). Use `release` (a little bounce)
  only after a drag or flick, started with the finger's velocity; decide flicks with
  `Physics.project`. Animate from the current on-screen value so motion can be interrupted.
- Haptics only through `rememberHaptics()`, and only for moments that matter (DEC-021).
- Tab screens pad their content by `TabBarClearance` at the bottom so the tab bar doesn't cover it.
- When a second feature needs the same control, move it here instead of copying it.
