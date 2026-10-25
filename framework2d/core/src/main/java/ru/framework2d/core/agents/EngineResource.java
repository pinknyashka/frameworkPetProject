package ru.framework2d.core.agents;

/**
 * Coarse-grained resource domains of the engine.
 *
 * <p>The enum order is the canonical lock order. When an agent needs more than
 * one domain, it must acquire them in ascending order. The registry enforces
 * this rule through {@link LockOrderException}.</p>
 */
public enum EngineResource {

    /** Asset loading/caches: maps, images, sprites, sounds, config. */
    RESOURCES("maps, images, sounds and asset caches"),

    /** Entity/component/data-context model, including map parsing. */
    MODEL("entities, components and data contexts"),

    /** Physics solvers, shapes and collision handlers. */
    PHYSICS("physics solver and collision handlers"),

    /** Game-logic handlers (fields, grids, players, respawns). */
    LOGIC("game-logic handlers"),

    /** Touch/pointer pipeline. */
    TOUCH("touch and pointer pipeline"),

    /** Rendering pipeline and graphical components. */
    GRAPHICS("rendering pipeline"),

    /** Audio playback. */
    SOUND("audio playback"),

    /** Tick/draw scheduler. */
    SCHEDULER("tick and draw scheduler");

    private final String description;

    EngineResource(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /**
     * Maps an engine/handler package to the domain it normally owns.
     * Handlers may override their scope explicitly, but this gives every
     * existing engine a safe default without editing it.
     */
    public static EngineResource forPackage(String packageName) {
        if (packageName == null) {
            return MODEL;
        }
        if (packageName.contains(".graphics")) {
            return GRAPHICS;
        }
        if (packageName.contains(".sound")) {
            return SOUND;
        }
        if (packageName.contains(".touches")) {
            return TOUCH;
        }
        if (packageName.contains(".physics")) {
            return PHYSICS;
        }
        if (packageName.contains(".logic")) {
            return LOGIC;
        }
        return MODEL;
    }
}
