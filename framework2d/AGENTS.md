# AGENTS.md

Operating contract for humans and AI agents working on `framework2d`.

Last updated: 2026-10-25. Read this before touching code, maps, assets, or tests. `README.MD` and `PROJECT_STRUCTURE.MD` are the current file/stack map; this file adds the rules that let several agents work at the same time without corrupting each other's work.

## 0. Current stack (the old ANT app is legacy)

- Active build: Gradle 8.7 multi-module `:core`, `:lwjgl3`, `:android`.
- Java source/target: `1.8`, built with JDK 17. libGDX `1.14.2`.
- Active engine: `core/src/main/java/ru/framework2d/**`.
- Active app shell: `core/src/main/java/ru/fungames/checkers/CheckersGame.java`, `lwjgl3/src/main/java/ru/fungames/checkers/desktop/DesktopLauncher.java`, `android/src/main/java/ru/fungames/checkers/AndroidLauncher.java`.
- Active runtime data: `assets/config.xml`, `assets/xml/*.xml`, `assets/Images/*`, `assets/Sounds/*`.
- Legacy paths: root `src/`, `gen/`, `res/`, `bin/`, `build.xml`, `.project`, `.classpath`, `project.properties`. Do not use them for active work; do not "fix" them unless the task is explicitly about the legacy shell.
- There is no general automated test suite, lint task, or CI. The lock layer has a targeted self-test task (`:core:lockSelfTest`); otherwise verification is compile + smoke run + targeted checks.
- All sources are UTF-8 without BOM. Keep the root Gradle `options.encoding = 'UTF-8'`.

## 1. Parallel-agent contract

The engine is a tree of handlers and components. Each handler owns a subsystem. Agents are independent writers, so the default is **one writer per resource, many readers, and explicit ownership of source files**.

Two mechanisms work together:

1. **Runtime semaphores** in `ru.framework2d.core.agents` stop the game runtime from touching a domain while another agent tests or mutates it.
2. **Source claims** in `plans/claims/` prevent two agents from editing the same files. They are advisory but mandatory by this contract.

The design and verification note is in `plans/parallel-agents-plan.md`.

### 1.1 Non-negotiable invariants

- Never edit a file that is already modified or untracked by another agent unless that agent has handed it off. Check `git status --short` first.
- Never touch a subsystem outside your declared scope without claiming it and announcing the reason.
- Never force-release, bypass, or delete another agent's lease or claim.
- Acquire locks in canonical `EngineResource` order; release in reverse order (try-with-resources does this).
- Do not hold a lock while sleeping, waiting for I/O, waiting for another lock, or running a long test.
- If a lock forbids your action, stop that action and wait or abort. Do not "work around" it by editing the resource anyway.
- Leave the working tree clean of your temporary files, maps, screenshots, and claims.

### 1.2 Agent identity

Every agent must have a stable, unique id. The lock registry uses it in diagnostics and reentrancy checks.

- Preferred: JVM property `-Dframework2d.agent=<id>`.
- Alternative: environment variable `FRAMEWORK2D_AGENT=<id>`.
- In tests: `EngineLocks.setAgentId("<id>")` on the acquiring thread before acquiring, and `EngineLocks.clearAgentId()` on teardown. The override is thread-local; use the JVM property for a process-wide id.
- Fallback: `agent-<pid>@<host>`.

Use a descriptive id such as `graphics-batching`, `sound-collision`, or `test-render-a`. Do not share an id between two agents: exclusive reentrancy is intentionally limited to the same agent and thread, but a shared id still makes diagnostics ambiguous.

### 1.3 Scope: what you own

The canonical domains are declared in `EngineResource`:

| Order | Domain | Owns | Typical packages/files |
|---|---|---|---|
| 1 | `RESOURCES` | config, maps, images, sprites, sounds, asset caches | `core/GameResources.java`, `compat/BitmapFactory.java`, asset directories |
| 2 | `MODEL` | entities, components, data contexts, map parsing | `core/Entity*`, `core/Component.java`, `core/MapParser.java`, `core/EnginesManager.java`, `data/**` |
| 3 | `PHYSICS` | solvers, shapes, collision handlers | `physics/**` |
| 4 | `LOGIC` | fields, grids, players, respawns | `logic/**` |
| 5 | `TOUCH` | touch/pointer pipeline | `touches/**` |
| 6 | `GRAPHICS` | canvas, graphical components, rendering | `graphics/**`, `compat/Canvas.java`, `compat/Paint.java` |
| 7 | `SOUND` | playback and audio components | `sound/**` |
| 8 | `SCHEDULER` | tick/draw handoff | `core/EnginesScheduler.java`, `core/Engine.java` iterate logic |

Order is the lock order. If you need more than one domain, acquire lower-numbered domains first. The registry throws `LockOrderException` instead of risking a deadlock.

A handler declares its scope in code. `ComponentsHandler` implements `AgentScoped`:

```java
EngineResource scope = handler.scopeResource(); // derived from the handler's package by default
String name = handler.scopeName();              // agent id + handler path
try (ResourceLease lease = handler.lockScope(LockMode.READ, "inspect state")) {
    // read-only work in this handler's domain
}
```

Override `scopeResource()` only when the handler genuinely spans domains, and document why in the task plan. A wrong scope is a bug, not a convenience.

**Shared files** require a source claim even for a one-line change:

- `AGENTS.md`, `README.MD`, `PROJECT_STRUCTURE.MD`
- root `build.gradle`, `settings.gradle`, `gradle.properties`
- `assets/xml/checkers_tree.xml` (the active map)
- `core/Engine.java`, `core/EnginesManager.java`, `core/EnginesScheduler.java`, `core/MapParser.java`
- `core/GameResources.java`

If a file is already dirty or untracked, assume another agent owns it until they say otherwise. In particular, uncommitted sound work touching `Engine.java`, `GameResources.java`, `checkers_tree.xml`, and `sound/**` belongs to the sound agent.

### 1.4 Lock modes and what they tell you

| Mode | Shared? | Blocks | Intended use |
|---|---|---|---|
| `READ` | yes, with other readers | writers and tests | inspect state, render a read-only view, compute a report |
| `WRITE` | no | all other holders | mutate components, parse a map, inject input |
| `TEST` | no | all other holders; marks a test isolation | pause a subsystem while an agent asserts or mutates |

A lease is a grant. Always close it, preferably with try-with-resources:

```java
try (ResourceLease lease = EngineLocks.acquire(
        EngineResource.GRAPHICS, LockMode.READ, "inspect sprite list")) {
    // read-only
}
```

When a lock is unavailable, the exception carries a `LockStatus` snapshot:

```java
try {
    // ...
} catch (LockTimeoutException e) {
    LockStatus status = e.status();
    Log.w("agent", e.describe());
    Log.w("agent", "owner=" + status.holders().get(0).owner()
            + " mode=" + status.holders().get(0).mode()
            + " reason=" + status.holders().get(0).reason()
            + " readAllowed=" + status.allowsRead()
            + " writeAllowed=" + status.allowsWrite());
}
```

`LockStatus` exposes holders, waiting agents, hold age, reentrancy count, the holder thread for exclusive locks, and:
- `allowsRead()` - a new reader is compatible with current holders.
- `allowsWrite()` - a new writer/test is compatible.
- `allows(LockMode)` - generic form.
- `heldBy(agentId)` - is this agent the holder?
- `describe()` - a single human-readable line.

Important nuance: `allowsRead()` describes holder compatibility. A read request can still queue behind a waiting writer, because the registry gives writers priority to prevent starvation. If you only need a read and the writer is queued, use `acquire(..., READ, ...)` and let it wait; do not spin on `isAvailable()`.

Other API:
- `EngineLocks.acquire(domain, mode, reason)` - wait up to `DEFAULT_WAIT_MS` (2 s).
- `EngineLocks.acquire(domain, mode, reason, timeoutMs)` - explicit wait.
- `EngineLocks.acquire(domain, mode, owner, reason, timeoutMs)` - explicit owner for harnesses.
- `EngineLocks.tryAcquire(domain, mode, reason)` - zero wait; throws `LockBusyException`.
- `EngineLocks.isAvailable(domain, mode)` - non-blocking probe.
- `EngineLocks.status(domain)`, `snapshot()`, `describe(domain)`, `describeAll()`.
- `EngineLocks.beginTestScope(reason, domains...)` - see 1.6.

### 1.5 Waiting protocol

When an operation is blocked:

1. Capture the diagnostic. Log `LockBusyException.describe()` or `LockTimeoutException.describe()`; do not swallow it.
2. Decide whether the intended action is read-only. If `status.allowsRead()` and your work does not mutate, you may proceed with a `READ` lease (or use the information you already have) without waiting for the writer.
3. If the action needs `WRITE`/`TEST`, wait. Prefer `EngineLocks.acquire(...)` with a bounded timeout over a hand-written sleep loop; it uses a condition variable and does not burn CPU.
4. If you must poll (for example across processes where only source claims exist), sleep 50-250 ms between checks, never while holding a lock. Exponential backoff up to a few seconds is acceptable.
5. On timeout, stop touching the domain. Report the owner, mode, reason, and how long you waited. Retry later, switch to an independent task, or request a handoff. Do not force-unlock.
6. Keep hold time short. A normal write lease should last milliseconds to a few seconds. A `TEST` scope should be bounded and released in `finally`; if a test needs longer, split it.
7. Do not auto-expire in-process leases. A leaked lease is a bug; `EngineLocks.describeAll()` shows it. A dead JVM releases everything because the registry is static.

Lock-order rules:

- Acquire in `EngineResource` enum order. Never acquire `MODEL` while holding `GRAPHICS`; the registry detects and rejects it.
- No read-to-write upgrade. Release the read lease first, then acquire write.
- `READ` is reentrant for the same owner. `WRITE`/`TEST` are reentrant only for the same owner on the same thread. A second thread of the same agent is not allowed to re-enter an exclusive lease.
- Leases are thread-confined. Close them on the thread that acquired them. Closing from another thread is logged and still releases, but the held-order tracking cannot follow it.

### 1.6 Testing boundaries

Tests are isolated with `AgentTestScope`, which acquires exclusive `TEST` leases on the domains that can interfere:

```java
try (AgentTestScope scope = EngineLocks.beginTestScope(
        "pause rendering and audio while asserting collision state",
        EngineResource.GRAPHICS,
        EngineResource.SOUND)) {
    // Only this agent may touch GRAPHICS/SOUND now.
    // Assertions and model reads/writes still need MODEL/PHYSICS leases if they mutate.
} finally {
    // scope.close() runs automatically; it releases in reverse order
}
```

Rules:

- Test only inside your declared scope. If a test needs another domain, add it to the scope explicitly and say why in the task plan.
- Pause only what interferes. Do not take `GRAPHICS`/`SOUND`/`TOUCH` for a pure logic test.
- If the test mutates entities/components, also take `MODEL` `WRITE`; do not rely on the test scope to cover model mutation.
- If the test loads assets, also take `RESOURCES`; if it advances physics, also take `PHYSICS`.
- Restore state, dispose textures/sounds, delete temporary maps/screenshots, and release scopes in `finally`.
- Runtime behavior while a test holds an exclusive lock is intentional:
  - `OutputDraw.draw` skips the frame (rate-limited warning) instead of drawing a partially mutated model.
  - `DirectTouches.toHandleEvent` drops the event instead of mutating a locked model.
  - `EnginesScheduler.startWork` pauses tick/draw handoff while `SCHEDULER` is unavailable.
  - `MapParser.parseMap` waits up to `BUILD_WAIT_MS` (30 s) for `MODEL`.
- A test that holds an exclusive lock for minutes will freeze the running game's rendering/audio. Keep it short or run it in a separate process with a separate map.

### 1.7 Source claims (repo-level semaphore)

Runtime locks cannot stop two agents editing the same Java file. Use a claim file in `plans/claims/` before any non-trivial work:

```text
plans/claims/graphics-batching.md

owner: graphics-batching
mode: write            # read | write | test
read-ok: false         # may another agent read the files while you work?
domains: GRAPHICS, MODEL
files:
  - core/src/main/java/ru/framework2d/graphics/OutputDraw.java
  - assets/xml/checkers_tree.xml
reason: skip draw while a test mutates the model
since: 2026-10-25T12:00:00Z
```

Rules:

1. Run `git status --short`; read existing claims; inspect dirty/untracked files before editing.
2. Create a claim for the area you will change. Use `mode: write` for edits; `read-ok: true` only if readers cannot observe half-written state.
3. If a conflicting write claim exists, wait or choose a non-overlapping task. Do not edit "just one line" in someone else's file.
4. Keep the claim short-lived. Update `since` if you continue; remove it after verification.
5. A claim older than 30 minutes with no matching process/activity is stale. Verify with `git status` and the agent's report, record the takeover in the claim, then proceed. Do not silently delete claims.
6. Claims are advisory. The code review still checks `git diff`; a claim does not excuse editing outside its `files` list.

#### Builds and compilations

Runtime semaphores live inside one JVM. Gradle tasks, desktop smoke runs, and APK builds are separate processes and write to the same `build/` directories, so they need their own coordination:

- Treat a build/smoke/APK run as an exclusive `BUILD` resource. Create `plans/claims/build-<agent>.md` with `mode: write`, the command, and the reason before starting it.
- Run one build/smoke at a time. If a build claim exists, wait for it to be removed instead of starting a second Gradle invocation; concurrent builds can block on Gradle's caches or mix outputs.
- Do not edit sources while a build or smoke run is in progress. A smoke run can load half-edited maps/assets; keep source claims and build claims separate.
- Release the build claim when the command exits, and report the exact command and result. If a build is still running after a long timeout, capture the process state and report it; do not kill another agent's Gradle daemon without a handoff.

### 1.8 Workflow checklist

1. **Pre-flight**: read this file and the relevant plan; `git status --short`; set your agent id; check `plans/claims/`; note pre-existing changes you do not own.
2. **Plan**: list domains, files, shared files, test scope, rollback, and verification commands. If you need another agent's file, request a handoff first.
3. **Claim**: write the claim; announce the scope in your task/report.
4. **Work**: acquire only the leases you need, for the shortest section possible. Use try-with-resources. Never edit outside the claim.
5. **Verify**: compile and run the narrowest check that proves your change. If you touched the lock layer, also run `.\gradlew.bat :core:lockSelfTest`.
6. **Cleanup**: release scopes/leases, remove temporary files and claims, check `git status` for your files only.
7. **Report**: files changed, domains/claims held, commands run, lock contention encountered, follow-ups.

### 1.9 Conflict resolution

- Read-only work yields to a writer. Tests yield to a production fix unless the test has a bounded isolation scope already announced.
- Two writers for one domain: the second waits or takes an independent task. Do not split a file between agents.
- Two writers for different domains: proceed in parallel, but still respect lock order if you need both.
- A lock held too long: capture `EngineLocks.describeAll()`, stop touching the domain, report, and ask the owner to release. Do not busy-spin.
- Suspected deadlock: check the `LockStatus` waiters and the canonical order. The registry rejects order violations, but a cross-process claim conflict must be resolved by handoff, not by force.
- If you inherit uncommitted work: treat it as owned. Either coordinate, or work on a disjoint area. Do not revert it to make your change easier.

## 2. Build and verify

Active commands:

```powershell
.\gradlew.bat :core:compileJava
.\gradlew.bat :core:lockSelfTest
.\gradlew.bat :lwjgl3:build
.\gradlew.bat :lwjgl3:run --args="--smoke"
.\gradlew.bat :lwjgl3:run --args="--smoke --map=binding_test"
.\gradlew.bat :android:assembleDebug
```

- `:core:lockSelfTest` runs the lock-layer scenarios (shared readers, writer rejection/wait, timeout diagnostics, upgrade rejection, lock order, test-scope isolation). It must print `ALL LOCK TESTS PASSED`.
- `--smoke` renders 180 frames and exits. A clean exit with no exceptions is the current smoke gate.
- Use `--map=<name>` to load another map under `assets/xml/`.
- The desktop module uses `assets/` as its working directory.
- The legacy `ant build` is not the active path. It compiles the old `src/`/`gen/` tree, hardcodes old SDK paths, and uses Java 1.5. Do not rely on it and do not "fix" it unless the task is explicitly about the legacy shell.
- There is no test suite, lint task, or CI. If you add a test, also add a runnable Gradle task so the next agent can execute it.

Useful log tags: `"core"`, `"Parser"`, `"Physical"`, `"Logical"`, `"Output"`, `"Resources"`, `"Sound"`, `"agents"`, `"framework2d"`.

Lock diagnostics are available without a debugger:

```java
Log.i("agent", EngineLocks.describeAll());
```

Run the JVM with `-Dframework2d.agent.verbose=true` to log every lock grant and release under the `agents` tag.

## 3. Engine architecture (current)

### 3.1 The map XML is the program

`GameCore.createObjectsModel` -> `MapParser.parseMap` walks an `XmlPullParser` and dispatches every tag. Adding a feature usually means editing `assets/xml/*.xml`, not Java.

- `<direct>` / `<handler>` / `<sub>` / `<interaction>` -> `EnginesManager.connectEngine`, which reflects a class by name from `package` + `subpackage` (relative to the parent engine's package) + `engineName`. A wrong name fails at parse time; only the logs show it.
- Any other tag -> `EnginesManager.createComponentInCurrentEntity`, which walks handlers until one reports `canCreateThisComponent(name)`. Matching is against the component's class simple name or its `public final static String SHORT_CLASS_NAME` (e.g. `pict`, `d_touch`, `zoomer`, `l_respawn`, `cell_group`). Tag names are case-sensitive; only XML attribute lookup (`Property.isNamed`) is case-insensitive.
- `<context name= value= [transferValue=]/>` inside a handler or component aliases data fields into shared `DataContext`s. This pub/sub wiring in `EnginesManager.engineCreationIsOver` / `setContext` is the least obvious part of the codebase; contexts merge by name across handlers, so map ordering can change behavior.
- Attribute values are evaluated as arithmetic (`width/24`, `100*8/9`) by `DataImplementator`; `<replace name= value=/>` defines substitution names, and `DisplayWidth` / `DisplayHeight` are seeded from the display. Prefix a value with `%s` to force a literal string.
- `<prototype>` defines a template; `HandlerRespawns.iterate` instantiates copies from `EntityStorage.createEntityByPrototype`. Only `<l_respawn>` inside a prototype registers it as a spawn source.
- Component data fields can be bound to live `Expression`s with `<dataName>_binding="..."` attributes. The expression is re-evaluated on every `commit()` of that data and written back before handler propagation; refs to entities parsed later are deferred and re-resolve at runtime, and bound fields survive prototype cloning. See `plans/binding-plan.md` and `assets/xml/binding_test.xml`.

### 3.2 Runtime loop and singletons

`EnginesScheduler` ticks the engine tree. The libGDX bridge calls `OutputDraw.draw`, which calls `enginesScheduler.overDraw()` to start the next tick. `Engine.resources`, `Engine.entityStorage`, `Engine.enginesManager`, and `Engine.enginesScheduler` are static singletons injected by the `EnginesManager` constructor. Do not instantiate engines directly.

`EnginesManager.PARALLEL_WORK` is a static flag read during construction. The active libGDX bridge currently leaves it at `false`; set it before creating `GameCore` if you need Timer-based parallel work. Changing it after construction has no effect.

### 3.3 Resources

- `GameResources` is the only loader. It reads internal `config.xml`, internal `xml/<name>.xml` maps, internal `Images/<name>.png` images, internal `Sounds/<name>.{ogg,wav}`, and external `sPart/...` overrides when the backend supports them.
- `getSprite` caches by sprite name only, ignoring the column/row counts of later callers. Two sprites sharing a bitmap with different grid dimensions get the first definition.
- `assets/config.xml` defines the directory names. On Android, an existing `sPart/config.xml` can shadow the bundled copy.
- `RESOURCES` is not yet integrated at every loader entry point. If you touch `GameResources` or load assets in a test, take a `RESOURCES` lease manually and see the follow-ups below.

### 3.4 Gotchas

- **UTF-8, no BOM.** The legacy cp1251 engine files were re-encoded; keep new files UTF-8. `options.encoding = 'UTF-8'` in the root `build.gradle` is load-bearing.
- Logging is extremely verbose (per property, per component, per tick). Grep the tags above; there is little other runtime feedback.
- Typos in identifiers are load-bearing: `DEAFULT_MAP_DIRECTORY`, `spriteCollumnsNum`, `depht`. Do not rename them without a repo-wide change.
- `advanced drawing` (shadows, reliefs, lights, clipping, gradients) uses libGDX-backed approximations. Visual correctness still needs manual checks.
- Other maps (`battle_checkers.xml`, `checkers_with_blocks.xml`) are legacy/preview data and may not match the current engine-tag schema. The active map is `checkers_tree`.

## 4. Current follow-ups for a stronger mechanism

These are known gaps, not permission to skip the rules above:

1. **Sound and resource integration.** `SoundPlayer.play` and `HandlerSounds.transferData` should take `SOUND` `READ`; `GameResources` cache mutation should take `RESOURCES` `WRITE`, with read leases around lookups. This is currently the sound agent's scope; coordinate before editing those files.
2. **Cross-process source lock CLI.** `plans/claims/` is an advisory file protocol. A small `tools/agent-lock` command (acquire/release/status/wait, JSON claims, stale detection, read/write modes) would make it enforceable across separate JVMs/processes.
3. **More lock tests and CI.** `core/src/test/java/ru/framework2d/core/agents/EngineLocksSelfTest.java` is runnable via `:core:lockSelfTest` and covers the main scenarios. Add stress/fairness cases, wire it into `check`/CI, and keep the Gradle task runnable for other agents.
4. **Wait-for graph.** The registry rejects lock-order violations, but a cross-agent cycle detector would improve diagnostics. Until then, respect the canonical order and keep hold times short.
5. **Lock metrics.** Expose contention counts and longest hold times through `EngineLocks.describeAll()` or a smoke flag.
6. **Optional map metadata.** A map-level `agentScope`/`test` attribute could declare which domains a scenario expects to isolate; not implemented today.

If you change the lock API or the rules in this file, update `PROJECT_STRUCTURE.MD` and add a note to `plans/` so the next agent can reproduce your verification.
