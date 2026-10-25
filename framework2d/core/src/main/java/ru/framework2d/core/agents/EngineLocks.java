package ru.framework2d.core.agents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import ru.framework2d.compat.Log;

/**
 * Process-wide semaphore registry for engine domains.
 *
 * <p>Parallel agents announce themselves with the {@code framework2d.agent}
 * system property (or {@code FRAMEWORK2D_AGENT} environment variable), or by
 * calling {@link #setAgentId(String)}. They then acquire the domains they need
 * before touching the engine. A busy lock carries the owner, mode, reason and
 * waiters, so callers can decide whether read-only work is still safe or
 * whether they must wait.</p>
 *
 * <p>Rules enforced here:</p>
 * <ul>
 *   <li>READ is shared; WRITE and TEST are exclusive.</li>
 *   <li>Multiple resources must be acquired in {@link EngineResource} enum order.</li>
 *   <li>READ-to-WRITE upgrades are rejected instead of deadlocking.</li>
 *   <li>Exclusive reentrancy is limited to the same agent and thread.</li>
 * </ul>
 */
public final class EngineLocks {

    private static final String LOG_TAG = "agents";
    private static final String AGENT_PROPERTY = "framework2d.agent";
    private static final String AGENT_ENV = "FRAMEWORK2D_AGENT";

    private static final String DEFAULT_AGENT =
            "agent-" + UUID.randomUUID().toString().substring(0, 8);

    /** Default wait for normal agent work. */
    public static final long DEFAULT_WAIT_MS = 2000L;

    /** Longer wait used when a test isolates several subsystems. */
    public static final long TEST_WAIT_MS = 10000L;

    /** Longer wait used for mandatory map/model construction. */
    public static final long BUILD_WAIT_MS = 30000L;

    /**
     * Set true, or run with {@code -Dframework2d.agent.verbose=true}, to log
     * every grant/release (very noisy; diagnostic only).
     */
    public static boolean VERBOSE = verboseFromProperty();

    private static final ThreadLocal<String> AGENT_OVERRIDE = new ThreadLocal<String>();

    private static final Map<EngineResource, State> STATES =
            new EnumMap<EngineResource, State>(EngineResource.class);

    static {
        for (EngineResource resource : EngineResource.values()) {
            STATES.put(resource, new State());
        }
    }

    private static final ThreadLocal<Deque<EngineResource>> HELD =
            new ThreadLocal<Deque<EngineResource>>() {
                @Override
                protected Deque<EngineResource> initialValue() {
                    return new ArrayDeque<EngineResource>();
                }
            };

    private EngineLocks() {
    }

    // ------------------------------------------------------------------ identity

    /**
     * Current agent identity. Prefer {@code -Dframework2d.agent=<id>} or
     * {@code FRAMEWORK2D_AGENT}; the fallback is unique per JVM process.
     */
    public static String currentAgent() {
        String override = AGENT_OVERRIDE.get();
        if (override != null && override.trim().length() > 0) {
            return override.trim();
        }
        String property = System.getProperty(AGENT_PROPERTY);
        if (property != null && property.trim().length() > 0) {
            return property.trim();
        }
        String env = System.getenv(AGENT_ENV);
        if (env != null && env.trim().length() > 0) {
            return env.trim();
        }
        return DEFAULT_AGENT;
    }

    /**
     * Overrides the agent id for the current thread. Use the
     * {@code framework2d.agent} system property for a process-wide id.
     */
    public static void setAgentId(String agentId) {
        AGENT_OVERRIDE.set(agentId);
    }

    public static void clearAgentId() {
        AGENT_OVERRIDE.remove();
    }

    private static boolean verboseFromProperty() {
        String value = System.getProperty("framework2d.agent.verbose");
        return value != null
                && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
    }

    // ------------------------------------------------------------------ acquire

    /** Acquire with the default wait and {@link #currentAgent()}. */
    public static ResourceLease acquire(EngineResource resource, LockMode mode, String reason) {
        return acquire(resource, mode, currentAgent(), reason, DEFAULT_WAIT_MS);
    }

    /** Acquire with an explicit timeout and {@link #currentAgent()}. */
    public static ResourceLease acquire(EngineResource resource, LockMode mode,
                                        String reason, long timeoutMs) {
        return acquire(resource, mode, currentAgent(), reason, timeoutMs);
    }

    /**
     * Acquire a resource. Waits up to {@code timeoutMs} (0 = do not wait).
     *
     * @throws LockBusyException  when no wait is allowed and the lock is busy
     * @throws LockTimeoutException when the wait expires or is interrupted
     * @throws LockOrderException when acquiring out of canonical order
     */
    public static ResourceLease acquire(EngineResource resource, LockMode mode,
                                        String owner, String reason, long timeoutMs) {
        if (resource == null) {
            throw new IllegalArgumentException("resource == null");
        }
        if (mode == null) {
            throw new IllegalArgumentException("mode == null");
        }
        if (owner == null || owner.trim().length() == 0) {
            owner = currentAgent();
        }
        if (reason == null || reason.trim().length() == 0) {
            reason = "unspecified";
        }

        checkOrder(resource);

        State state = STATES.get(resource);
        long started = System.currentTimeMillis();
        long deadline = timeoutMs <= 0L ? started : safeAdd(started, timeoutMs);

        state.mutex.lock();
        try {
            if (canAcquire(state, owner, mode)) {
                boolean read = grantLocked(state, owner, mode, reason);
                keepHeld(resource);
                if (VERBOSE) {
                    Log.d(LOG_TAG, "granted " + resource + " " + mode + " to " + owner
                            + " for '" + reason + "'");
                }
                return new ResourceLease(resource, mode, owner, reason, read);
            }

            if (mode != LockMode.READ && hasReadLock(state, owner)) {
                LockStatus status = snapshotLocked(state, resource);
                throw new IllegalStateException("Lock upgrade is not supported: " + owner
                        + " holds READ on " + resource + " and requested " + mode
                        + "; release the read lease first. " + status.describe());
            }

            if (timeoutMs <= 0L) {
                LockStatus status = snapshotLocked(state, resource);
                throw new LockBusyException("Cannot acquire " + resource + " " + mode
                        + " for " + owner + ": " + status.describe(),
                        resource, mode, status);
            }

            Waiter waiter = new Waiter(owner, mode, reason, started, Thread.currentThread().getName());
            state.waiters.add(waiter);
            try {
                while (!canAcquire(state, owner, mode)) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0L) {
                        state.waiters.remove(waiter);
                        LockStatus status = snapshotLocked(state, resource);
                        throw new LockTimeoutException("Timed out after " + timeoutMs
                                + " ms waiting for " + resource + " " + mode + " for " + owner
                                + ": " + status.describe(),
                                resource, mode, status,
                                System.currentTimeMillis() - started);
                    }
                    try {
                        state.changed.await(remaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        state.waiters.remove(waiter);
                        LockStatus status = snapshotLocked(state, resource);
                        throw new LockTimeoutException("Interrupted while waiting for "
                                + resource + " " + mode + " for " + owner + ": "
                                + status.describe(), resource, mode, status,
                                System.currentTimeMillis() - started);
                    }
                }

                boolean read = grantLocked(state, owner, mode, reason);
                keepHeld(resource);
                if (VERBOSE) {
                    Log.d(LOG_TAG, "granted after wait " + resource + " " + mode + " to "
                            + owner + " for '" + reason + "'");
                }
                return new ResourceLease(resource, mode, owner, reason, read);
            } finally {
                state.waiters.remove(waiter);
                state.changed.signalAll();
            }
        } finally {
            state.mutex.unlock();
        }
    }

    /** Non-blocking acquire; throws {@link LockBusyException} when unavailable. */
    public static ResourceLease tryAcquire(EngineResource resource, LockMode mode, String reason) {
        return acquire(resource, mode, currentAgent(), reason, 0L);
    }

    /** True when the current agent would be granted the requested mode now. */
    public static boolean isAvailable(EngineResource resource, LockMode mode) {
        if (resource == null || mode == null) {
            return false;
        }
        State state = STATES.get(resource);
        state.mutex.lock();
        try {
            return canAcquire(state, currentAgent(), mode);
        } finally {
            state.mutex.unlock();
        }
    }

    // ------------------------------------------------------------------ status

    public static LockStatus status(EngineResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource == null");
        }
        State state = STATES.get(resource);
        state.mutex.lock();
        try {
            return snapshotLocked(state, resource);
        } finally {
            state.mutex.unlock();
        }
    }

    public static List<LockStatus> snapshot() {
        List<LockStatus> result = new ArrayList<LockStatus>(EngineResource.values().length);
        for (EngineResource resource : EngineResource.values()) {
            result.add(status(resource));
        }
        return result;
    }

    public static String describe(EngineResource resource) {
        return status(resource).describe();
    }

    public static String describeAll() {
        StringBuilder builder = new StringBuilder();
        for (EngineResource resource : EngineResource.values()) {
            builder.append(describe(resource)).append('\n');
        }
        return builder.toString();
    }

    /** Starts an exclusive test isolation scope for the listed domains. */
    public static AgentTestScope beginTestScope(String reason, EngineResource... resources) {
        return new AgentTestScope(reason, resources);
    }

    // ------------------------------------------------------------------ release

    static void release(ResourceLease lease) {
        if (lease.ownerThread() != Thread.currentThread()) {
            Log.w(LOG_TAG, "Lease released from a different thread ("
                    + Thread.currentThread().getName() + "): " + lease
                    + "; acquire and close leases on the same thread");
        }
        State state = STATES.get(lease.resource());
        state.mutex.lock();
        try {
            if (lease.isRead()) {
                Reader reader = state.readers.get(lease.owner());
                if (reader != null) {
                    reader.count--;
                    if (reader.count <= 0) {
                        state.readers.remove(lease.owner());
                    }
                } else {
                    Log.w(LOG_TAG, "Read lease without reader record: " + lease);
                }
            } else {
                if (state.exclusiveOwner == null
                        || !state.exclusiveOwner.equals(lease.owner())) {
                    Log.w(LOG_TAG, "Exclusive lease without matching owner: " + lease);
                } else {
                    state.exclusiveCount--;
                    if (state.exclusiveCount <= 0) {
                        state.exclusiveOwner = null;
                        state.exclusiveMode = null;
                        state.exclusiveReason = null;
                        state.exclusiveSince = 0L;
                        state.exclusiveThread = null;
                        state.exclusiveCount = 0;
                    }
                }
            }

            if (VERBOSE) {
                Log.d(LOG_TAG, "released " + lease.resource() + " " + lease.mode()
                        + " by " + lease.owner());
            }
            state.changed.signalAll();
        } finally {
            state.mutex.unlock();
        }
        releaseHeld(lease.resource());
    }

    // ------------------------------------------------------------------ internals

    private static boolean canAcquire(State state, String owner, LockMode mode) {
        if (state.exclusiveOwner != null) {
            return state.exclusiveOwner.equals(owner)
                    && state.exclusiveThread == Thread.currentThread();
        }
        if (mode == LockMode.READ) {
            // A reader that already owns a read lease may re-enter even while a
            // writer is queued; otherwise new readers queue behind writers.
            if (state.readers.containsKey(owner)) {
                return true;
            }
            for (Waiter waiter : state.waiters) {
                if (waiter.mode.isExclusive()) {
                    return false;
                }
            }
            return true;
        }
        return state.readers.isEmpty();
    }

    /**
     * @return true when the grant is a shared read lease, false for exclusive.
     */
    private static boolean grantLocked(State state, String owner, LockMode mode, String reason) {
        long now = System.currentTimeMillis();

        if (state.exclusiveOwner != null) {
            state.exclusiveCount++;
            return false;
        }

        if (mode == LockMode.READ) {
            Reader reader = state.readers.get(owner);
            if (reader == null) {
                reader = new Reader(owner, reason, now);
                state.readers.put(owner, reader);
            }
            reader.count++;
            return true;
        }

        state.exclusiveOwner = owner;
        state.exclusiveMode = mode;
        state.exclusiveReason = reason;
        state.exclusiveSince = now;
        state.exclusiveThread = Thread.currentThread();
        state.exclusiveCount = 1;
        return false;
    }

    private static boolean hasReadLock(State state, String owner) {
        return state.readers.containsKey(owner);
    }

    private static LockStatus snapshotLocked(State state, EngineResource resource) {
        List<LockStatus.Holder> holders = new ArrayList<LockStatus.Holder>();
        if (state.exclusiveOwner != null) {
            holders.add(new LockStatus.Holder(
                    state.exclusiveOwner,
                    state.exclusiveMode,
                    state.exclusiveReason,
                    state.exclusiveSince,
                    state.exclusiveCount,
                    state.exclusiveThread == null ? null : state.exclusiveThread.getName()));
        } else {
            for (Reader reader : state.readers.values()) {
                holders.add(new LockStatus.Holder(
                        reader.owner,
                        LockMode.READ,
                        reader.reason,
                        reader.since,
                        reader.count,
                        null));
            }
        }

        List<LockStatus.Waiter> waiters = new ArrayList<LockStatus.Waiter>();
        for (Waiter waiter : state.waiters) {
            waiters.add(new LockStatus.Waiter(
                    waiter.owner, waiter.mode, waiter.reason, waiter.since));
        }

        return new LockStatus(resource, holders, waiters);
    }

    private static void checkOrder(EngineResource requested) {
        Deque<EngineResource> held = HELD.get();
        if (held.isEmpty()) {
            return;
        }
        EngineResource top = held.peekLast();
        if (top != null && requested.ordinal() < top.ordinal()) {
            throw new LockOrderException(requested, top);
        }
    }

    private static void keepHeld(EngineResource resource) {
        HELD.get().addLast(resource);
    }

    private static void releaseHeld(EngineResource resource) {
        Deque<EngineResource> held = HELD.get();
        if (held.isEmpty()) {
            return;
        }
        if (held.peekLast() == resource) {
            held.removeLast();
        } else {
            held.removeLastOccurrence(resource);
        }
    }

    private static long safeAdd(long value, long delta) {
        long result = value + delta;
        return result < 0L ? Long.MAX_VALUE : result;
    }

    private static final class State {
        final ReentrantLock mutex = new ReentrantLock(true);
        final Condition changed = mutex.newCondition();
        final LinkedHashMap<String, Reader> readers = new LinkedHashMap<String, Reader>();
        final List<Waiter> waiters = new ArrayList<Waiter>();

        String exclusiveOwner;
        LockMode exclusiveMode;
        String exclusiveReason;
        long exclusiveSince;
        int exclusiveCount;
        Thread exclusiveThread;
    }

    private static final class Reader {
        final String owner;
        final String reason;
        final long since;
        int count;

        Reader(String owner, String reason, long since) {
            this.owner = owner;
            this.reason = reason;
            this.since = since;
            this.count = 0;
        }
    }

    private static final class Waiter {
        final String owner;
        final LockMode mode;
        final String reason;
        final long since;
        final String threadName;

        Waiter(String owner, LockMode mode, String reason, long since, String threadName) {
            this.owner = owner;
            this.mode = mode;
            this.reason = reason;
            this.since = since;
            this.threadName = threadName;
        }
    }
}
