# PROJECT_STRUCTURE.MD — framework2d

Current structure after the migration from Eclipse/ANT Android-only sources to a Gradle + libGDX multi-module layout.

The active stack is:

- Gradle wrapper `8.7`.
- libGDX `1.14.2`.
- Java source/target `1.8`, built with JDK 17.
- Android Gradle Plugin `8.5.2`.
- Android compile SDK `34`, target SDK `34`, min SDK `21`.
- Root Gradle Java compile encoding: `UTF-8` (all sources re-encoded from the legacy cp1251).

The old Eclipse/ANT files are still present, but they are no longer the active build path.

## Migration status

Active targets:

- `:core` — shared engine code, compat shims, input bridge, game runtime bridge.
- `:lwjgl3` — x86 desktop app.
- `:android` — Android APK app.

Current build verification:

```powershell
.\gradlew.bat :core:compileJava
.\gradlew.bat :lwjgl3:build
.\gradlew.bat :android:assembleDebug
```

Current desktop smoke command:

```powershell
.\gradlew.bat :lwjgl3:run --args="--smoke"
```

The desktop smoke run currently uses `checkers_tree` as the default map and exits after 180 frames.

## Top level

| File | Description |
|---|---|
| `AGENTS.md` | Current operating contract for parallel agents: active Gradle stack, resource domains, runtime semaphores (`EngineLocks`), lock modes, waiting rules, test isolation, source claims, and build/verify commands. |
| `README.MD` | Human-facing overview of the migrated stack, modules, build/run commands, resources, map format, gotchas. |
| `PROJECT_STRUCTURE.MD` | Current file/directory map. |
| `build.gradle` | Root Gradle build. Adds AGP classpath, common repos, project version `0.2`, `appName`, `gdxVersion = '1.14.2'`, and sets Java compile encoding to `UTF-8`. |
| `settings.gradle` | Includes modules `:core`, `:lwjgl3`, and `:android`. |
| `gradle.properties` | Gradle daemon/JVM settings and AndroidX enablement: `android.useAndroidX=true`. |
| `gradlew`, `gradlew.bat` | Gradle wrapper scripts. |
| `gradle/wrapper/gradle-wrapper.properties` | Wrapper configuration, Gradle `8.7`. |
| `local.properties` | Machine-local Android SDK pointer. Must point `sdk.dir` to a valid Android SDK on the current machine. |
| `plans/binding-plan.md` | Plan + verification result for the data-binding feature (`<dataName>_binding` map attributes); implemented and smoke-verified, no longer pending. |
| `plans/parallel-agents-plan.md` | Design + verification note for the runtime resource locks (`ru.framework2d.core.agents`), source/build claims, integration points, and follow-ups. |
| `changelog.txt` | Legacy dated changelog. Russian, UTF-8. |
| `AndroidManifest.xml` | Legacy Eclipse-era Android manifest. Not used by the active Android module. |
| `build.xml` | Legacy ANT build file. Not used by Gradle. |
| `project.properties` | Legacy ADT/Android target file. Not used by Gradle. |
| `.project`, `.classpath`, `.settings/` | Legacy Eclipse metadata. Not used by Gradle. |
| `lint.xml`, `proguard-project.txt` | Legacy Eclipse/ADT configuration remnants. |
| `libs/android-support-v4.jar` | Legacy Android support JAR. Not used by the active Gradle modules. |
| `src/` | Legacy source root after engine relocation. Currently contains the old `MainActivity`/`PlayGround` shell only; not used by Gradle modules. |
| `gen/` | Legacy aapt-generated `R.java`/`BuildConfig.java`. Not used by Gradle modules. |
| `res/` | Legacy Android resources from the old app shell. Not used by the migrated runtime path. |
| `bin/` | Stale Eclipse/ADT artifacts. Ignore when reasoning about source state. |

## `core/` — shared engine module

### `core/build.gradle`

| File | Description |
|---|---|
| `core/build.gradle` | Java library module. Adds libGDX core, `xmlpull`, and runtime XPP3 for desktop/JVM XML parsing. Source/target compatibility is `1.8`. Defines the runnable `lockSelfTest` JavaExec task for the parallel-agent lock layer. |

### `core/src/main/java/ru/fungames/checkers/`

| File | Description |
|---|---|
| `CheckersGame.java` | libGDX `Game` runtime bridge. Creates `GameCore`, parses `DEFAULT_MAP = "checkers_tree"`, scans `GameCore.enginesManager.lstEngines` for `GraphicalPeriphery` and `TouchPeriphery`, creates `OrthographicCamera`/`SpriteBatch`/`BitmapFont`/compat `Canvas`, installs `TouchBridge`, and calls `periphery.draw(canvas)` each frame. Supports `--smoke` frame-exit behavior through `exitAfterFrames`. |

### `core/src/main/java/ru/framework2d/gdx/`

| File | Description |
|---|---|
| `TouchBridge.java` | libGDX `InputAdapter` bridge. Tracks pointers, converts libGDX y-up coordinates to the engine's y-down space, and builds compat `MotionEvent` objects for `TouchPeriphery`. |

### `core/src/main/java/ru/framework2d/compat/`

Compatibility package replacing Android API classes used by the original engine. These are intentionally named like the old Android classes so engine imports could be shifted mechanically.

| File | Description |
|---|---|
| `Bitmap.java` | Replacement for `android.graphics.Bitmap`; wraps a libGDX `Texture`. |
| `BitmapFactory.java` | Minimal replacement for `android.graphics.BitmapFactory`; decodes absolute file paths into `Bitmap`/`Texture`. |
| `Canvas.java` | Central replacement for `android.graphics.Canvas`. Uses `SpriteBatch`, y-down projection, affine transform folding, bitmap blits, rectangles, paths, text, and save/restore behavior. Advanced Android drawing features are approximated where needed. |
| `Color.java` | Static color helpers/constants used by the engine. |
| `ColorFilter.java` | Base color-filter type. |
| `GradientDrawable.java` | Approximated replacement for Android gradient drawables. |
| `LightingColorFilter.java` | Approximated replacement for Android lighting color filters. |
| `Log.java` | Replacement for `android.util.Log`, delegating to `Gdx.app`. |
| `Matrix.java` | 2D affine matrix replacement used by shadow/layer code. |
| `MotionEvent.java` | Replacement for `android.view.MotionEvent`; stores action, pointer coordinates, pointer IDs, and pointer-index encoding. |
| `Paint.java` | Replacement for `android.graphics.Paint`; stores color, alpha, style, stroke, text size, anti-alias flag, and color filter. Some effects are approximate. |
| `Path.java` | Polygon/path replacement. Supports `moveTo`, `lineTo`, `close`, rect/circle approximation, matrix transform, and point access. |
| `Rect.java` | Integer rectangle replacement. |
| `RectF.java` | Float rectangle replacement. |
| `Region.java` | Minimal replacement for `android.graphics.Region`; primarily a compatibility stub for the current drawing path. |

### `core/src/main/java/ru/framework2d/core/` — engine core

| File | Description |
|---|---|
| `Engine.java` | Abstract engine-tree node; holds static singletons (`resources`, `entityStorage`, `enginesManager`, `enginesScheduler`); propagates work state; serial/parallel iteration. **Russian comments (UTF-8).** |
| `EnginesManager.java` | Central registry: reflectively instantiates engines from tags, dispatches component creation to handlers, merges named `DataContext`s, holds `PARALLEL_WORK`. **Russian comments (UTF-8).** |
| `EnginesScheduler.java` | Root engine ticking the engine tree and finishing draw passes through `overDraw()`. |
| `ComponentsHandler.java` | Abstract `Engine` + `Factory` that owns component classes; creates components by tag name, wires contexts, connects entity components to handlers. |
| `GameCore.java` | Top-level migrated facade. Constructs `GameResources`, `EntityStorage`, `EnginesManager`, and `MapParser`; parses a chosen map through `createObjectsModel(String mapName)`. No Android `SurfaceView` dependency. |
| `MapParser.java` | XML map parser and tag dispatcher. Reads entity/prototype tags, `<replace>`, `<context>`, engine tags, and component tags. Uses `com.badlogic.gdx.Gdx.graphics.getWidth()`/`getHeight()` for `DisplayWidth`/`DisplayHeight`. **Russian comments (UTF-8).** |
| `Entity.java` | Game object grouping components; sorts graphical/touch/physical/logical components; reads `name`/`single`; clones prototypes. |
| `EntityStorage.java` | Holds parsed entities and prototypes; tracks current entity nesting; spawns instances from prototypes. |
| `Component.java` | Abstract component base; holds position, entity link, data fields, handlers; applies XML attributes through `setPropertyData`; supports reflective prototype cloning. |
| `Property.java` | Pairs an XML attribute name with a typed `DataInterface` value; case-insensitive `isNamed`. |
| `TagOptions.java` | Per-tag options bag: tag name, depth, properties; case-insensitive `findPropertyByName`. |
| `DataContext.java` | Named pub/sub hub recording linked component classes and handlers. |
| `DataCommunicationContext.java` | Pairs one component data-field name with a shared `DataContext` and optional `transferValue`. **Russian comments (UTF-8).** |
| `Factory.java` | Component creator interface: `canCreateThisComponent`, `createComponent`, `connectComponent`, `setContextToComponent`, `getContexts`. |
| `InteractionHandler.java` | Collision interaction interface: `isInteractionPossible`, `isInteractionHappened`, `toHandleInteraction`. |
| `Sprite.java` | Sprite/grid wrapper: bitmap plus columns/rows grid for frame lookup. |
| `DataImplementator.java` | Static evaluator turning XML attribute strings into typed `DataInterface` values, or arithmetic/comparison/boolean results. |
| `Expression.java` | Runtime expression engine resolving `$object.component.data` references through the live entity model. |
| `ExpressionCalculator.java` | Static parse-time calculator for pure numeric attribute expressions. |
| `GameResources.java` | Migrated libGDX resource loader. Reads internal `config.xml`, internal `xml/<name>.xml` maps, internal `Images/<name>.png` images, internal `Sounds/<name>.ogg|wav` sounds, optional external `sPart/...` overrides, and creates placeholder/fallback textures. Still caches sprites by name only. |

Removed from the active core module:

- `GameSurface.java` — the old Android `SurfaceView` integration point. The libGDX render bridge now drives drawing and input.

### `core/src/main/java/ru/framework2d/core/agents/` — parallel-agent locks

Runtime semaphores used by concurrent agents. Domains are fixed by `EngineResource`; `EngineLocks` is the process-wide registry. See `AGENTS.md` for the operating rules.

| File | Description |
|---|---|
| `EngineResource.java` | Engine domains and canonical lock order (`RESOURCES`, `MODEL`, `PHYSICS`, `LOGIC`, `TOUCH`, `GRAPHICS`, `SOUND`, `SCHEDULER`) plus package-to-domain default. |
| `LockMode.java` | `READ` (shared), `WRITE` (exclusive mutation), `TEST` (exclusive test isolation). |
| `AgentScoped.java` | Interface for handlers/engines that declare their owned `EngineResource` and diagnostic scope name. |
| `EngineLocks.java` | Acquire/tryAcquire/isAvailable/status/snapshot/describe registry with owner, reason, waiters, timeouts, reentrancy, writer preference, lock-order enforcement, and upgrade rejection. |
| `ResourceLease.java` | AutoCloseable lock grant; close it on the acquiring thread, preferably with try-with-resources. |
| `LockStatus.java` | Immutable holder/waiter snapshot; exposes `allowsRead()`, `allowsWrite()`, `allows(mode)`, `heldBy(agent)`, `describe()`. |
| `LockBusyException.java` | Non-blocking acquisition failure with the `LockStatus` snapshot. |
| `LockTimeoutException.java` | Timed/interrupted wait failure with the snapshot and waited time. |
| `LockOrderException.java` | Rejects acquisition out of canonical `EngineResource` order. |
| `AgentTestScope.java` | Acquires exclusive `TEST` leases on several domains in order and releases them in reverse. |

`core/src/test/java/ru/framework2d/core/agents/EngineLocksSelfTest.java` is a plain-`main` verification harness for the lock layer; run it with `.\gradlew.bat :core:lockSelfTest`.

### `core/src/main/java/ru/framework2d/data/` — typed data values

All implement or extend `DataInterface`; `commit()` dispatches pub/sub to linked component handlers.

| File | Description |
|---|---|
| `DataInterface.java` | Abstract base for data values; holds name/context and pub/sub handler lists. |
| `Numeral.java` | `int` value. |
| `Fractional.java` | `float` value. |
| `DoubleFractional.java` | `double` value. |
| `Bool.java` | boolean value. |
| `Text.java` | string value. |
| `Trigger.java` | Valueless marker data used as event trigger. |
| `Point2.java` | 2D point with bounds/line checks. |
| `Point3.java` | 3D point with per-axis property parsing. |
| `Position2.java` | 2D position plus rotation. |
| `Position3.java` | 3D position plus Euler angles. |
| `Vector2.java` | 2D vector math. **Russian comments (UTF-8).** |
| `Vector3.java` | 3D vector math. **Russian comments (UTF-8).** |
| `Movement2.java` | 2D movement vector plus speed/rotation. |
| `Movement3.java` | 3D movement vector plus speed/rotations. |
| `Geometry.java` | Abstract geometry base. |
| `GeometryBlock.java` | Block geometry parsed from `:height`/`:width`/`:depth` properties. |
| `GeometryVerticalCylinder.java` | Vertical cylinder geometry parsed from `:height`/`:radius`. |
| `Area.java` | 3D AABB/sphere region with containment and area helpers. |

### `core/src/main/java/ru/framework2d/graphics/` — rendering

| File | Description |
|---|---|
| `GraphicalPeriphery.java` | Interface for drawing engines. Implemented by `OutputDraw`; discovered by `CheckersGame`. |
| `OutputDraw.java` | Root rendering engine. Holds `lstGraphicals`, draws them through compat `Canvas`/`Paint`, and calls `enginesScheduler.overDraw()`. |
| `OutputGraphicalInterface.java` | Abstract base for graphical components: position, layer, transparency, visibility, transform, draw, child recursion. |

#### `graphics/animation/`

| File | Description |
|---|---|
| `HandlerAnimation.java` | Animation handler; starts/stops animations through triggers and steps values. **Russian comments (UTF-8).** |
| `OutputGraphicalAnimation.java` | Animated scalar component (`animation`). |

#### `graphics/cameras/`

| File | Description |
|---|---|
| `HandlerCameras.java` | Camera handler; creates pointer cameras, applies camera transform to canvas, converts touches to camera-local coordinates. |
| `OutputGraphicalCamera.java` | Camera component (`camera`); zoom, eye position, touchability, coordinate conversion. **Russian comments (UTF-8).** |
| `WorldPointerCamera.java` | Camera-bound pointer (`cam_pointer`). |

#### `graphics/draw/`

| File | Description |
|---|---|
| `HandlerDraw.java` | Registers graphicals with the periphery, wires subhandlers, dispatches draw/data transfers. |
| `SubBasicGraphical.java` | Applies shared graphical properties such as position, local position, transparency, and visibility. |

#### `graphics/draw/sprite/`

| File | Description |
|---|---|
| `OutputGraphicalSprite.java` | Sprite/picture component (`pict`). Uses load-bearing typo `spriteCollumnsNum`. |
| `SubSpriteDraw.java` | Loads sprite through `GameResources` and draws current frame. |

#### `graphics/draw/text/`

| File | Description |
|---|---|
| `OutputGraphicalText.java` | Text component (`text`); content, font size, color. |
| `SubDrawText.java` | Renders text through compat `Canvas`/`Paint`. |

#### `graphics/draw/shadows/`

| File | Description |
|---|---|
| `OutputGraphicalLight.java` | Light component (`light`). |
| `SubLight.java` | Light subhandler; uses compat gradient/lighting approximations. |
| `OutputGraphicalRelief.java` | Relief sprite component (`relief`). |
| `SubRelief.java` | Draws relief sprite frames with light color filtering. |
| `OutputGraphicalShadow.java` | Shadow component (`shadow`); geometry + projection path. |
| `SubShadows.java` | Computes/draws projected shadow paths using compat `Path`/`Paint`. |
| `OutputGraphicalShadowLayer.java` | Shadow layer component (`shadow_layer`). |
| `SubShadowLayers.java` | Accumulates/draws shadow paths per layer using compat clipping approximations. |

These advanced drawing features currently rely on libGDX-backed approximations and may still need visual refinement.

### `core/src/main/java/ru/framework2d/physics/` — physics

| File | Description |
|---|---|
| `IntervalPhysics.java` | Fixed-timestep physics engine; parses `ph_unit`, `interval_limit`, `impulse_limit`. |
| `PhysicalInterface.java` | Abstract physics component base; movement and enabled state. |

#### `physics/collisions/`

| File | Description |
|---|---|
| `HandlerShapesCollisions.java` | Shape collision handler; pairwise collision resolution and impulses. |
| `InteractionShapes.java` | Base shape-pair collision response. **Russian comments (UTF-8).** |
| `PhysicalShape.java` | Abstract collision shape; AABB, static flag, contact points, impulse, and `collisionSpeed` for the last collision. |

#### `physics/collisions/circles/`

| File | Description |
|---|---|
| `PhysicalCircle.java` | Circle shape (`circle`). |
| `InteractionCircles.java` | Circle-circle collision handler; publishes relative collision speed for listeners such as sound volume. **Russian comments (UTF-8).** |

#### `physics/collisions/blocks/`

| File | Description |
|---|---|
| `PhysicalBlock.java` | Rotatable block shape (`block`); uses load-bearing typo `depht`. |
| `InteractionBlocks.java` | Block-block collision handler. |

#### `physics/collisions/circleblock/`

| File | Description |
|---|---|
| `InteractionCirclesBlocks.java` | Circle-block collision handler. **Russian comments (UTF-8).** |

#### `physics/collisions/particular/` and `particularblock/`

| File | Description |
|---|---|
| `PhysicalParticular.java` | Point-particle shape. |
| `InteractionParticularBlock.java` | Particular-block interaction; currently largely disabled/commented out. |

#### `physics/medium/`

| File | Description |
|---|---|
| `HandlerMediums.java` | Body/medium handler. |
| `PhysicalBody.java` | Body component (`body`) with roughness. |
| `PhysicalMedium.java` | Surface/medium component (`surface`) with slowing effect. |
| `InteractionMediumBody.java` | Damps body movement while inside a medium. |

### `core/src/main/java/ru/framework2d/logic/` — game logic

| File | Description |
|---|---|
| `DirectLogicStandart.java` | Standard logic engine. |
| `LogicalInterface.java` | Base class for logic components. |

#### `logic/fields/`

| File | Description |
|---|---|
| `LogicalUnit.java` | Unit component (`unit`) tracked by fields. |
| `LogicalField.java` | Field component (`field`) for containment. |
| `HandlerFields.java` | Field/unit handler. |

#### `logic/grids/`

| File | Description |
|---|---|
| `LogicalGrid.java` | Grid component (`grid`). |
| `LogicalCellItem.java` | Cell-bound item component (`item`). |
| `LogicalCellGroup.java` | Named group of grid cells (`cell_group`). |
| `HandlerGrids.java` | Grid/item/cell-group handler. |
| `CellGrid.java` | Plain grid-cell data object. |

#### `logic/players/`

| File | Description |
|---|---|
| `LogicalPlayer.java` | Player component (`player`). |
| `LogicalOwnership.java` | Ownership component (`ownership`). |
| `HandlerPlayers.java` | Player/ownership/turn/win logic handler. **Russian comments (UTF-8).** |

#### `logic/respawns/`

| File | Description |
|---|---|
| `LogicalRespawner.java` | Respawner component (`respawner`). |
| `LogicalRespawn.java` | Spawn point inside prototype (`l_respawn`). |
| `HandlerRespawns.java` | Handler instantiating prototype copies and respawning entities. |

### `core/src/main/java/ru/framework2d/touches/` — touch input

| File | Description |
|---|---|
| `DirectTouches.java` | Touches engine implementing `TouchPeriphery`; receives compat `MotionEvent`s from `TouchBridge`. |
| `InputTouchInterface.java` | Base class for touch input components. |
| `MotionEventListener.java` | Interface for objects handling a compat `MotionEvent`. |
| `TouchPeriphery.java` | Interface defining the touch periphery contract. |
| `WorldPointer.java` | World-space pointer component (`pointer`). |

#### `touches/areas/`

| File | Description |
|---|---|
| `TouchableArea.java` | Base touchable area (`touchable`). |
| `SubPointer.java` | Sub-handler linking pointers to touchable areas. |
| `HandlerTouchSelect.java` | Main touch-area handler. |

#### `touches/areas/drag/`

| File | Description |
|---|---|
| `TouchableAreaDrag.java` | Drag area (`drag`). |
| `SubDrag.java` | Drag sub-handler. |

#### `touches/areas/direct/`

| File | Description |
|---|---|
| `TouchableAreaDirect.java` | Direct touch area (`d_touch`). |
| `SubDirectedTouch.java` | Direct shot/rotate vector calculation; uses libGDX display size. |

#### `touches/areas/switcher/`

| File | Description |
|---|---|
| `TouchableAreaSwitcher.java` | Toggle area (`switcher`). |
| `SubSwitcher.java` | Switcher sub-handler. |

#### `touches/areas/zoom/`

| File | Description |
|---|---|
| `TouchableAreaZoom.java` | Zoom/pinch area (`zoomer`). |
| `SubZoomTouches.java` | Zoom sub-handler. |

#### `touches/screen/`

| File | Description |
|---|---|
| `TouchFinger.java` | Per-finger state (`finger`). |
| `HandlerTouchScreen.java` | Screen touch handler implementing `MotionEventListener`. |

#### `touches/motion/`

| File | Description |
|---|---|
| `TouchMotion.java` | Tap/motion component (`motion`). |
| `HandlerMotion.java` | Motion handler; currently mostly dormant/commented-out behavior. |

### `core/src/main/java/ru/framework2d/sound/`

| File | Description |
|---|---|
| `DirectSound.java` | Sound engine node for maps. |
| `HandlerSounds.java` | Plays collision-triggered audio and scales volume by `PhysicalShape.collisionSpeed`. |
| `AudioComponent.java` | `audio` component; defines `sound`, `volume`, `fullVolumeSpeed`, and `minVolume`. |
| `SoundPlayer.java` | Playback helper backed by `GameResources.getSound(...)` and libGDX `Sound`. |

`assets/xml/sound_test.xml` is a smoke map for collision sound and volume-ramp verification: `--smoke --map=sound_test`.

## `lwjgl3/` — desktop module

### `lwjgl3/build.gradle`

| File | Description |
|---|---|
| `lwjgl3/build.gradle` | Java/application module. Depends on `:core`, `gdx-backend-lwjgl3`, and desktop natives. Main class: `ru.fungames.checkers.desktop.DesktopLauncher`. `run` uses `assets/` as working directory for libGDX internal assets. |

### `lwjgl3/src/main/java/ru/fungames/checkers/desktop/`

| File | Description |
|---|---|
| `DesktopLauncher.java` | Desktop entry point. Creates `Lwjgl3Application` with a `CheckersGame`. Window is 800×600, 60 FPS. Supports `--smoke` by setting `CheckersGame.exitAfterFrames = 180`. |

## `android/` — Android module

### `android/build.gradle`

| File | Description |
|---|---|
| `android/build.gradle` | Android application module. Namespace/application ID `ru.fungames.checkers`, `versionCode 2`, `versionName 0.2`, compile SDK 34, build tools `34.0.0`, min SDK 21, target SDK 34. Java source/target 1.8. Assets are sourced from root `assets/`. Depends on `:core` and `gdx-backend-android`; excludes desktop-only `xmlpull`/`xpp3` from `:core` because Android provides `org.xmlpull.v1`. Copies libGDX native artifacts into `jniLibs/<abi>/` before build. |

### `android/AndroidManifest.xml`

| File | Description |
|---|---|
| `android/AndroidManifest.xml` | Active Android manifest. Requires OpenGL ES 2.0. Launcher activity is `ru.fungames.checkers.AndroidLauncher`, portrait orientation, exported. It does not use the old `MainActivity`/`PlayGround` activities. |

### `android/res/`

| File | Description |
|---|---|
| `android/res/values/strings.xml` | Minimal active Android string resources: `app_name`, `my_package_name`. `my_package_name` is retained for compatibility/legacy code paths, but the migrated libGDX `GameResources` loader no longer uses Android `R` lookup. |

### `android/src/main/java/ru/fungames/checkers/`

| File | Description |
|---|---|
| `AndroidLauncher.java` | libGDX `AndroidApplication` launcher. Calls `initialize(new CheckersGame(), config)`. |

### `android/jniLibs/`

Generated native library directory populated by the Gradle `copyAndroidNatives` task. Expected ABI folders include `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.

### `android/build/`

Gradle/AGP outputs. Not source. Current debug APK path:

```text
android/build/outputs/apk/debug/android-debug.apk
```

## `assets/` — runtime game data

### `assets/config.xml`

Runtime config consumed by migrated `GameResources`. Defines Maps/Images/Sounds directory names.

### `assets/xml/`

| File | Description |
|---|---|
| `checkers_tree.xml` | Currently the default active runtime map. Uses the current engine-tag schema: `<direct package="..." engineName="...">`, `<handler subpackage="..." engineName="...">`, `<interaction subpackage="..." engineName="...">`, `<sub subpackage="..." engineName="...">`. |
| `binding_test.xml` | Smoke/verification map for the data-binding feature (`<*_binding` attributes on component data fields); loadable via `--map=binding_test` in the desktop run. |
| `sound_test.xml` | Smoke/verification map for collision sound and impact-speed volume ramp; loadable via `--map=sound_test` in the desktop run. |
| `battle_checkers.xml` | Legacy/preview map kept for compatibility and reference. Uses an older engine-tag style (`name = "physical"`, etc.) that does not currently match `EnginesManager.connectEngine`'s reflective schema. Not loaded by the current bridge. |
| `checkers_with_blocks.xml` | Legacy/preview block-phase map kept for compatibility and reference. Uses engine-tag attributes/styles that do not currently match the active runtime bridge's parser expectations. Not loaded by the current bridge. |

### `assets/Images/`

Game images copied from old drawable resources so they can be loaded by libGDX `FileHandle` paths.

| File | Description |
|---|---|
| `blackblock.png` | Static block barrier texture. |
| `board.png` | Checkerboard background. |
| `bonehead.png` | Piece variant sprite. |
| `checker_preview.png` | Map preview thumbnail. |
| `checker_relief_sprite.png` | Relief sprite. |
| `firehead_sprite.png` | Piece variant sprite with frame grid. |
| `image_not_found.png` | Fallback image. |
| `label.png` | Text label background. |
| `no_data.png` | Placeholder for missing data/preview. |
| `point.png` | Point/marker graphic. |
| `power.png` | Power/ability icon. |
| `valid_checker.png` | Valid-move highlight sprite. |
| `valid_checker_sprite.png` | Valid-move sprite with frame grid. |
| `whiteblock.png` | Static block barrier texture. |

### `assets/Sounds/`

| File | Description |
|---|---|
| `bump.ogg` | Collision bump sound used by `checkers_tree` and `sound_test`. |

## Legacy Eclipse/ANT remnants

These files are retained during migration but are not used by the Gradle modules.

| Path | Description |
|---|---|
| `build.xml` | Old ANT build. Hardcodes old ADT/Android SDK paths and Java `1.5`. |
| `.project`, `.classpath`, `.settings/` | Old Eclipse/ADT project metadata. |
| `project.properties` | Old Android target metadata (`android-16`). |
| `AndroidManifest.xml` | Old root manifest declaring legacy `MainActivity`/`PlayGround`. |
| `src/ru/fungames/checkers/MainActivity.java` | Old Android map-picker UI. Not compiled by current Gradle modules. |
| `src/ru/fungames/checkers/PlayGround.java` | Old Android game activity. Not compiled by current Gradle modules. |
| `gen/` | Old aapt-generated sources. Not compiled by current Gradle modules. |
| `res/` | Old Android resources. Active Android resources are under `android/res`; game images are under `assets/Images`. |
| `libs/android-support-v4.jar` | Old support library. |
| `bin/` | Stale build artifacts. |

## Key agent notes

- **Prefer Gradle commands** now: `:core:compileJava`, `:lwjgl3:run`, `:lwjgl3:build`, `:android:assembleDebug`.
- **Do not use old `src/` paths for the active engine.** The active engine tree lives under `core/src/main/java/ru/framework2d`.
- **Keep Android shims compatible with the original engine.** The compat package exists so engine code could be ported without rewriting every graphics/touch/logging call in one pass.
- **Advanced drawing is approximate.** `Canvas`, `Paint`, `GradientDrawable`, `LightingColorFilter`, `Path`, and clipping/shadow/relief behavior are currently compatibility approximations backed by libGDX.
- **The active runtime map is `checkers_tree`.** The current bridge does not yet reproduce the old `MainActivity` map picker.
- **Other maps remain present as legacy data.** They may require map-schema updates before they can be loaded by the current engine reflection path.
- **Sound is active.** The `sound` package contains the collision-triggered playback path; collision volume is scaled by `PhysicalShape.collisionSpeed`.
- **Keep files UTF-8 (no BOM).** The root Gradle build compiles with `UTF-8`; the legacy cp1251 engine comments and `changelog.txt` have been re-encoded.
- **No tests/lint/CI.** Verification is compile + desktop smoke + APK assembly + on-device run.
