# framework2d

A data-driven 2D game engine. Games are defined almost entirely in XML map files; the Java code is the engine that parses and runs them. The shipped game (Battle Checkers) is now represented by a thin libGDX runtime bridge plus three XML map definitions.

The project has been shifted from the old Eclipse/ANT Android-only layout to a Gradle multi-module layout:

- `core` — shared engine and runtime code.
- `lwjgl3` — x86 desktop launcher.
- `android` — Android launcher/APK module.

The old Eclipse/ANT files are still present for historical reference, but the active build path is Gradle.

## Current stack

- Gradle wrapper: `8.7`.
- Java source/target: `1.8`; recommended build JDK: JDK 17.
- libGDX: `1.14.2`.
- Desktop backend: `gdx-backend-lwjgl3` + `gdx-platform:natives-desktop`.
- Android backend: `gdx-backend-android` + `gdx-platform:natives-<abi>`.
- Android Gradle Plugin: `8.5.2`.
- Android compile SDK: `34`.
- Android target SDK: `34`.
- Android min SDK: `21`.
- Java compile encoding: `windows-1251`, configured in the root `build.gradle`, because legacy engine comments are cp1251.

## Requirements

- JDK 17, or another JDK compatible with Gradle 8.7 and AGP 8.5.2.
- Android SDK with `platforms;android-34` and `build-tools;34.0.0` if building the Android module.
- A machine-local `local.properties` file pointing `sdk.dir` to your Android SDK.

## Project layout

```text
core/src/main/java/       Shared framework2d engine + libGDX compat/input/runtime bridge
lwjgl3/src/main/java/     Desktop launcher
android/src/main/java/    Android launcher
android/AndroidManifest.xml Active Android manifest

assets/config.xml         Engine runtime config
assets/xml/*.xml          Game maps
assets/Images/*.png       Game images loaded by the migrated resource loader

src/                      Legacy Eclipse-era Java shell (MainActivity/PlayGround); not used by Gradle
gen/                      Legacy aapt-generated sources; not used by Gradle
res/                      Legacy Android resources; not used by the migrated runtime path
bin/                      Stale Eclipse/ADT build artifacts; ignore
plans/                    Working plans, e.g. planned data-binding work
build.xml, .project, .classpath, project.properties
                          Legacy Eclipse/ANT metadata; not used by Gradle
```

## Build

Compile the shared engine:

```powershell
.\gradlew.bat :core:compileJava
```

Build the desktop module:

```powershell
.\gradlew.bat :lwjgl3:build
```

Run the desktop application:

```powershell
.\gradlew.bat :lwjgl3:run
```

Run the desktop smoke test, which exits after a fixed number of frames:

```powershell
.\gradlew.bat :lwjgl3:run --args="--smoke"
```

Build the Android debug APK:

```powershell
.\gradlew.bat :android:assembleDebug
```

Expected artifacts:

```text
lwjgl3/build/libs/lwjgl3-0.2.jar
android/build/outputs/apk/debug/android-debug.apk
```

The Android module copies libGDX native libraries into `android/jniLibs/<abi>/` during `preBuild`.

## Runtime bridge

The old Android `SurfaceView` path has been replaced by a libGDX render bridge:

- `core/src/main/java/ru/fungames/checkers/CheckersGame.java`
  - Creates `GameCore`.
  - Parses the default map.
  - Finds `GraphicalPeriphery` and `TouchPeriphery` engines.
  - Renders through a compat `Canvas` backed by `SpriteBatch`.
- `core/src/main/java/ru/framework2d/gdx/TouchBridge.java`
  - Converts libGDX touch callbacks into engine `MotionEvent` objects.
  - Converts y-up libGDX screen coordinates to the engine's y-down coordinate space.
- `core/src/main/java/ru/framework2d/compat/`
  - Drop-in replacements for the Android API classes used by the engine, such as `Canvas`, `Paint`, `Bitmap`, `Color`, `Path`, `Matrix`, `Region`, `GradientDrawable`, and `MotionEvent`.
  - `Log` delegates to `Gdx.app`.

The current active desktop launcher loads `checkers_tree` by default:

```java
CheckersGame.DEFAULT_MAP = "checkers_tree"
```

The old `MainActivity` map picker and `PlayGround` activity remain under `src/` as legacy code; they are not wired into the Gradle/libGDX runtime yet.

## Resources

The migrated `GameResources` loader uses libGDX `FileHandle`s:

- Config: internal `config.xml`.
- Maps: internal `xml/<name>.xml`.
- Images: internal `Images/<name>.png`, then internal root `<name>.png`.
- Optional external overrides under `sPart/...` when the runtime/storage supports it.
- Missing images fall back to `image_not_found.png`, or to a 1×1 placeholder texture if even that cannot be loaded.

The old Android asset-to-SD copying behavior from the pre-migration app is not part of the current runtime bridge.

## Map format

`GameCore.createObjectsModel(...)` -> `MapParser.parseMap(...)` walks an `XmlPullParser` and dispatches tags:

- `<direct>` / `<handler>` / `<sub>` / `<interaction>` — engines, reflected by class name (`package` + `subpackage` + `engineName`, relative to the parent engine's package).
- Any other tag — components; a handler claiming `canCreateThisComponent(name)` creates it. Tag names are case-sensitive and matched against the class simple name or its `SHORT_CLASS_NAME`.
- `<context name= value= [transferValue=]/>` aliases data fields into shared `DataContext`s.
- Attribute values are evaluated as arithmetic (`width/24`, `100*8/9`), with `<replace name= value=/>` substitutions; `DisplayWidth`/`DisplayHeight` are pre-seeded from `Gdx.graphics.getWidth()` / `Gdx.graphics.getHeight()`. Prefix a value with `%s` to force a literal string.
- `<prototype>` defines a template; `HandlerRespawns` instantiates copies from it.

The active runtime currently uses `checkers_tree.xml` because its engine tags match the current `EnginesManager.connectEngine` schema. The other shipped maps remain as data files, but they use older/preview engine tag styles and are not loaded by the current bridge.

## Verification state

Current verified local build/compile commands:

```powershell
.\gradlew.bat :core:compileJava
.\gradlew.bat :lwjgl3:build
.\gradlew.bat :android:assembleDebug
```

The desktop smoke run currently loads `checkers_tree`, renders frames, exits, and reports no exceptions in the smoke log. Visual correctness of advanced drawing effects (shadows, reliefs, lights, clipping, gradients) still needs manual comparison/refinement.

There is no automated test suite, lint task, or CI configured.

## Sound

The `core/src/main/java/ru/framework2d/sound/` package exists but is currently empty. The project does not currently contain sound-effect implementation. Sound work is intentionally out of scope for the migration pass.

## Gotchas

- **Legacy source encoding is Windows-1251, not UTF-8.** Several relocated engine files under `core/src/main/java/ru/framework2d` contain Russian comments in cp1251. The root Gradle build forces Java compilation encoding to `windows-1251`. New bridge/shim files are plain ASCII.
- `GameResources.getSprite` caches by sprite name only — two sprites sharing a bitmap but with different grid dimensions get the first definition.
- Logging remains verbose. Useful tags include `"core"`, `"Parser"`, `"Physical"`, `"Logical"`, `"Output"`, `"Resources"`, and `"framework2d"`.
- Typos in identifiers are load-bearing (`DEAFULT_MAP_DIRECTORY`, `spriteCollumnsNum`, `depht`) — do not rename without a repo-wide change.
- The root `AndroidManifest.xml`, `build.xml`, `.project`, `.classpath`, `project.properties`, `src/`, `gen/`, and `res/` are legacy paths kept during migration. The active Android module uses `android/AndroidManifest.xml` and `android/res`.
