package ru.framework2d.core.agents;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A granted lock. Always close it on the thread that acquired it:
 *
 * <pre>
 * try (ResourceLease lease = EngineLocks.acquire(EngineResource.GRAPHICS,
 *         LockMode.READ, "inspect sprite list")) {
 *     // read-only work
 * }
 * </pre>
 *
 * <p>Closing twice is harmless; closing from another thread is logged and
 * still releases the lock so an exception cannot leak it.</p>
 */
public final class ResourceLease implements AutoCloseable {

    private final EngineResource resource;
    private final LockMode mode;
    private final String owner;
    private final String reason;
    private final long acquiredAtMillis;
    private final boolean readLease;
    private final Thread ownerThread;
    private final AtomicBoolean released = new AtomicBoolean(false);

    ResourceLease(EngineResource resource, LockMode mode, String owner,
                  String reason, boolean readLease) {
        this.resource = resource;
        this.mode = mode;
        this.owner = owner;
        this.reason = reason;
        this.acquiredAtMillis = System.currentTimeMillis();
        this.readLease = readLease;
        this.ownerThread = Thread.currentThread();
    }

    public EngineResource resource() {
        return resource;
    }

    public LockMode mode() {
        return mode;
    }

    public String owner() {
        return owner;
    }

    public String reason() {
        return reason;
    }

    public long acquiredAtMillis() {
        return acquiredAtMillis;
    }

    public boolean isRead() {
        return readLease;
    }

    public boolean isReleased() {
        return released.get();
    }

    public Thread ownerThread() {
        return ownerThread;
    }

    @Override
    public void close() {
        if (released.compareAndSet(false, true)) {
            EngineLocks.release(this);
        }
    }

    @Override
    public String toString() {
        return resource + " " + mode + " by " + owner + " for '" + reason
                + "'" + (isReleased() ? " (released)" : "");
    }
}
