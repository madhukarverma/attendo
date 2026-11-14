# core/designsystem/

Look and feel shared by every screen. Sources are in
`src/main/kotlin/com/attendo/core/designsystem/`.

| File | What it is | Open it for |
|---|---|---|
| `theme/Theme.kt` | Fonts, colour palettes for each theme, `AttendoColors`, `AttendoTheme`, contrast helpers | colours, fonts, adding a theme |
| `motion/Motion.kt` | Animation curves and durations for each Animations mode, `LocalAnimationMode` | how fast or bouncy things move |
| `motion/AppMotionScale.kt` | App-wide animation speed, follows the system animator scale | global animation speed |
| `motion/IosPress.kt` | `Modifier.iosPress`: the press-and-shrink effect | tap feedback |
| `motion/BounceScroll.kt` | `BounceContainer`: stretchy overscroll | scroll bounce |
| `motion/LargeTitle.kt` | Large titles that collapse into a top bar on scroll | screen headers |
| `component/Components.kt` | Cards, buttons, speech bubble, squiggle, animated numbers, small glyphs | shared building blocks |
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
- Tab screens pad their content by `TabBarClearance` at the bottom so the tab bar doesn't cover it.
- When a second feature needs the same control, move it here instead of copying it.
