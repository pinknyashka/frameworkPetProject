package ru.framework2d.core.agents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of one resource's holders and waiters.
 *
 * <p>This is the information an agent gets when a resource is busy: who holds
 * it, in which mode, for what reason, since when, and who is already waiting.</p>
 */
public final class LockStatus {

    private final EngineResource resource;
    private final List<Holder> holders;
    private final List<Waiter> waiters;

    LockStatus(EngineResource resource, List<Holder> holders, List<Waiter> waiters) {
        this.resource = resource;
        this.holders = Collections.unmodifiableList(new ArrayList<Holder>(holders));
        this.waiters = Collections.unmodifiableList(new ArrayList<Waiter>(waiters));
    }

    public EngineResource resource() {
        return resource;
    }

    public List<Holder> holders() {
        return holders;
    }

    public List<Waiter> waiters() {
        return waiters;
    }

    public boolean isFree() {
        return holders.isEmpty();
    }

    /**
     * True when the current holders are compatible with a new reader. A queued
     * writer may still be granted first; use {@link EngineLocks#acquire} to wait.
     */
    public boolean allowsRead() {
        if (holders.isEmpty()) {
            return true;
        }
        for (Holder holder : holders) {
            if (holder.mode().isExclusive()) {
                return false;
            }
        }
        return true;
    }

    /** True when the current holders are compatible with a new writer/test. */
    public boolean allowsWrite() {
        return holders.isEmpty();
    }

    public boolean allows(LockMode mode) {
        if (mode == null) {
            return false;
        }
        return mode == LockMode.READ ? allowsRead() : allowsWrite();
    }

    public boolean heldBy(String owner) {
        if (owner == null) {
            return false;
        }
        for (Holder holder : holders) {
            if (owner.equals(holder.owner())) {
                return true;
            }
        }
        return false;
    }

    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append(resource.name());

        if (holders.isEmpty()) {
            builder.append(": free (readable, writable)");
        } else {
            builder.append(": held by ");
            for (int i = 0; i < holders.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                Holder holder = holders.get(i);
                builder.append(holder.owner());
                builder.append(" [").append(holder.mode());
                if (holder.reentrant() > 1) {
                    builder.append(" x").append(holder.reentrant());
                }
                if (holder.threadName() != null) {
                    builder.append(", thread ").append(holder.threadName());
                }
                builder.append("]");
                builder.append(" for '").append(holder.reason()).append("'");
                builder.append(" (").append(age(holder.sinceMillis())).append(")");
            }
        }

        if (!waiters.isEmpty()) {
            builder.append("; waiters=").append(waiters.size()).append(" [");
            for (int i = 0; i < waiters.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                Waiter waiter = waiters.get(i);
                builder.append(waiter.owner()).append(" ").append(waiter.mode());
                builder.append(" '").append(waiter.reason()).append("'");
                builder.append(" (").append(age(waiter.sinceMillis())).append(")");
            }
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return describe();
    }

    private static String age(long sinceMillis) {
        long millis = Math.max(0L, System.currentTimeMillis() - sinceMillis);
        if (millis < 1000L) {
            return millis + " ms";
        }
        return String.format(java.util.Locale.US, "%.1f s", millis / 1000.0);
    }

    public static final class Holder {

        private final String owner;
        private final LockMode mode;
        private final String reason;
        private final long sinceMillis;
        private final int reentrant;
        private final String threadName;

        Holder(String owner, LockMode mode, String reason, long sinceMillis,
               int reentrant, String threadName) {
            this.owner = owner;
            this.mode = mode;
            this.reason = reason;
            this.sinceMillis = sinceMillis;
            this.reentrant = reentrant;
            this.threadName = threadName;
        }

        public String owner() {
            return owner;
        }

        public LockMode mode() {
            return mode;
        }

        public String reason() {
            return reason;
        }

        public long sinceMillis() {
            return sinceMillis;
        }

        public int reentrant() {
            return reentrant;
        }

        public String threadName() {
            return threadName;
        }
    }

    public static final class Waiter {

        private final String owner;
        private final LockMode mode;
        private final String reason;
        private final long sinceMillis;

        Waiter(String owner, LockMode mode, String reason, long sinceMillis) {
            this.owner = owner;
            this.mode = mode;
            this.reason = reason;
            this.sinceMillis = sinceMillis;
        }

        public String owner() {
            return owner;
        }

        public LockMode mode() {
            return mode;
        }

        public String reason() {
            return reason;
        }

        public long sinceMillis() {
            return sinceMillis;
        }
    }
}
