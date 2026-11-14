---
name: animation-performance
description: Keeping animations and gestures smooth and cheap, in Jetpack Compose first (with the web equivalents). Use when writing or reviewing anything that moves - springs, drags, progress bars, idle loops, blur and translucent layers, scroll-linked effects - or when chasing jank, dropped frames, battery drain or slow start-up. Companion to the apple-design skill, which says what motion should feel like; this one keeps it inside the frame budget.
---

# Animation Performance

Fluid motion (see `../apple-design/SKILL.md`) only feels fluid if every frame arrives on time.
At 60 Hz a frame has 16.7 ms; at 120 Hz, 8.3 ms, shared between your code, layout, drawing and
the GPU. The rule behind everything here: **while something moves, do as little work per frame
as possible, and do it as late in the pipeline as possible.**

## 1. Know the three phases

Compose turns state into pixels in three phases, each cheaper than the one before:

| Phase | Runs when | Cost per frame if an animation triggers it |
| --- | --- | --- |
| Composition | a state read in a composable body changes | re-runs the function, diffs the UI tree, then layout and draw |
| Layout | a state read in `measure`/`layout`/`offset {}` changes | measures and places the subtree, then draw |
| Drawing | a state read in a draw or `graphicsLayer {}` block changes | redraws only that layer |

An animation should reach the **drawing** phase only. The web has the same ladder: style/layout
(width, top, margin) → paint (color, shadow) → composite (`transform`, `opacity`). Animate
compositor properties; Compose's `graphicsLayer` is the equivalent of a composited layer.

## 2. Read animated values late (deferred reads)

The single biggest win. Reading `anim.value` in a composable body recomposes it every frame.
Reading it inside a lambda that runs in layout or draw does not.

```kotlin
// Recomposes the whole card ~60–120 times a second while it moves.
val dx = offsetX.value
Card(Modifier.offset(x = dx.dp).alpha(1f - abs(dx) / 500f))

// Only the layer is redrawn.
Card(Modifier.graphicsLayer {
    translationX = offsetX.value
    alpha = 1f - abs(offsetX.value) / 500f
})
```

- Pass **lambdas** (`() -> Float`) down to children, not values, when a child needs a moving
  number: `Stamp(alpha = { offsetX.value / threshold })`, read inside its `graphicsLayer {}`.
- Derived numbers (progress from a scroll position, collapse fraction) go through
  `derivedStateOf` and are still read late.
- Use the lambda forms: `Modifier.offset { IntOffset(...) }`, not `offset(x, y)`;
  `drawBehind { drawRect(color) }`, not `background(animatedColor)`, for colours that animate
  continuously. A short colour cross-fade on a small composable (a chip, a tab) is fine as it is.

## 3. Animate transforms and drawing, not layout

- **Progress bars, meters, charts:** draw the fill with `drawRoundRect` in a `Canvas` or
  `drawBehind`, from a draw-phase read. Animating `fillMaxWidth(fraction)` or `height(x.dp)`
  re-measures every frame.
- **Moving or resizing:** prefer `translationX/Y`, `scaleX/Y` and `rotationZ` in a
  `graphicsLayer`. Size animations (`animateContentSize`, `expandHorizontally`) re-layout every
  frame: keep them for small, short transitions where nothing transform-based can work.
- **Fading:** `graphicsLayer { alpha = ... }`. `Modifier.alpha(animated)` read in composition
  recomposes.

## 4. Idle and looping animations

Something that animates forever (a breathing mascot, a shimmer, a wave) keeps the frame clock
running and the screen redrawing at the full refresh rate, which costs battery even when
nothing changes.

- Run loops only while they mean something: a timer wave while the timer runs, not when paused.
- Read loop values only in draw blocks (`Canvas`, `drawWithContent`), never in composition.
- Honour "animations off": with `AnimationMode.NONE` (or the system's "Remove animations",
  `ValueAnimator.areAnimatorsEnabled() == false`) draw a still frame and start no loop.
- Avoid slow oscillations around 0.2 Hz and full-screen movement; they're also an accessibility
  problem (see the apple-design skill, §14).

## 5. Layers, blur and transparency cost

- `graphicsLayer` is cheap to *update* but each layer has memory and composition cost. Don't
  wrap every item of a long list in its own layer for no reason.
- `alpha < 1` on a layer with overlapping children may force an offscreen buffer. Use
  `CompositingStrategy.ModulateAlpha` when children don't overlap; `Offscreen` only when you
  need blend modes or correct group alpha.
- **Blur** (`RenderEffect`/`BlurEffect`, CSS `backdrop-filter`) costs in proportion to the
  blurred area and radius. Keep it to small floating surfaces (tab bar, a sheet header), never a
  whole screen. Record the backdrop once and reuse it (`GraphicsLayer.record`) rather than
  drawing the content twice. Older devices (below Android 12) get a solid tint.
- Shadows on moving elements re-render each frame; a static shadow on a layer that only
  translates is fine.

## 6. Gestures

- Update the moving value with `snapTo` (or a plain state the layer reads), not by recomposing.
- Track velocity with `VelocityTracker` on the *accumulated offset*, not the pointer position
  inside a layer that moves with the finger (that position barely changes).
- Keep work in `onDrag` tiny: no allocation-heavy maths, no repository calls, no logging.
- Hit testing and state for the drop target or threshold: compute from stored coordinates;
  don't trigger layout to find out.

## 7. Lists and scroll-linked effects

- Give lazy items stable `key`s and a `contentType`, so scrolling reuses compositions.
- Scroll-linked visuals (collapsing title, edge fade, parallax) read the scroll state in draw
  or layout lambdas, through `derivedStateOf` when they map it to a 0–1 fraction.
- Never start an animation per item on every scroll; animate on change only (`LaunchedEffect(target)`).

## 8. Stability and skipping

- UI state classes should be immutable (`val`s, read-only collections). With the Kotlin 2.0.20+
  Compose compiler, strong skipping is on by default, so unchanged parameters skip recomposition.
- Remember lambdas that capture changing state only when profiling shows they matter.
- Hoist expensive derived data (`remember(input) { compute(input) }`) out of draw blocks; a draw
  block runs every frame of an animation.

## 9. Start-up and the first frames

- Release builds with R8 (`isMinifyEnabled`, `isShrinkResources`) and baseline profiles (Compose
  libraries ship theirs; a project profile from Macrobenchmark helps more). Jank in the first
  seconds is often JIT, not your code.
- Don't run heavy work on the main thread at start: file reads, CSV parsing and JSON go to
  `Dispatchers.IO`.

## 10. Measure, then optimise

Change things because a measurement says so, not on a hunch; then measure again.

| Question | Tool |
| --- | --- |
| Is this composable recomposing while it animates? | Android Studio Layout Inspector, recomposition counts |
| Which frames were late, and why? | System trace / Perfetto (`Trace` sections), Android Studio profiler |
| Frame times on a device | `adb shell dumpsys gfxinfo <package> framestats`; Developer options › Profile HWUI rendering |
| Jank in the field or in tests | JankStats, Macrobenchmark `FrameTimingMetric` |
| Overdraw | Developer options › Debug GPU overdraw |
| Web equivalents | DevTools Performance panel, Rendering › Paint flashing / Layer borders; add `will-change` only after a measured problem |

Test on a slow, older phone with animations at normal speed, at 60 Hz and at 120 Hz.

## Quick reference

| Situation | Do | Avoid |
| --- | --- | --- |
| Moving value | `graphicsLayer { translationX = v.value }` | `offset(v.value.dp)` read in composition |
| Child needs a moving number | pass `() -> Float` | pass `Float` |
| Progress fill | draw it (`drawRoundRect`) | `fillMaxWidth(animated)` |
| Animated colour, continuous | `drawBehind { drawRect(c) }` | `background(c)` |
| Idle loop | draw-phase reads, pause when idle, none when animations are off | loops that never stop |
| Blur | small surface, record once, solid fallback < API 31 | full-screen blur |
| Lists | `key`, `contentType` | per-item layers or animations on scroll |
| Optimising | measure first | guessing |

## In this project (Attendo)

- Springs and timing: `core/designsystem/.../motion/Motion.kt`; gesture maths:
  `core/model/.../Physics.kt`; haptics: `motion/Haptics.kt`.
- Draw-phase patterns already in use: `iosPress` (highlight, dim and scale in `drawWithContent`
  and `graphicsLayer`), `BounceContainer` (offset in a layer), `CollapsingTopBar` (fraction read
  in draw), `FocusDial` and the mascots (loop values read only in `Canvas`), the card deck
  (`graphicsLayer`, lambdas for the stamps).
- Progress bars: use `ProgressBar` in `component/Controls.kt` (animated in the draw phase).
- Blur: `component/Glass.kt` records the screens once and blurs them only inside the tab bar.
- Every animation must respect `LocalAnimationMode` (`NONE` = no motion, no loops).
