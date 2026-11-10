package ru.framework2d.core.agents;

/**
 * Access requested on an {@link EngineResource}.
 */
public enum LockMode {

    /**
     * Shared access. Several readers may hold the same resource at once.
     * A reader must not mutate the resource.
     */
    READ(false),

    /**
     * Exclusive mutation. No other reader or writer may hold the resource.
     */
    WRITE(true),

    /**
     * Exclusive test isolation. Semantically identical to {@link #WRITE} for
     * conflict resolution, but marks the holder as an automated test so the
     * runtime can stop touching the resource instead of merely serialising
     * with it.
     */
    TEST(true);

    private final boolean exclusive;

    LockMode(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isExclusive() {
        return exclusive;
    }
}
