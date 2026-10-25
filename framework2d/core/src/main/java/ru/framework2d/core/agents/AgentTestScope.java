package ru.framework2d.core.agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.framework2d.compat.Log;

/**
 * Holds exclusive {@link LockMode#TEST} locks on a set of engine domains for
 * the duration of a test or experiment.
 *
 * <pre>
 * try (AgentTestScope scope = EngineLocks.beginTestScope(
 *         "verify sprite batching", EngineResource.GRAPHICS, EngineResource.SOUND)) {
 *     // rendering is paused; only this thread may touch GRAPHICS/SOUND
 * }
 * </pre>
 *
 * <p>Runtime sound playback does not take a SOUND lease yet, so a test scope
 * on SOUND currently isolates other agents, not the audio thread.</p>
 *
 * <p>Domains are acquired in {@link EngineResource} order and released in
 * reverse order. If one acquisition fails, already-granted domains are
 * released before the exception escapes.</p>
 */
public final class AgentTestScope implements AutoCloseable {

    private static final String LOG_TAG = "agents";

    private final String agent;
    private final String reason;
    private final List<ResourceLease> leases = new ArrayList<ResourceLease>();
    private boolean closed;

    public AgentTestScope(String reason, EngineResource... resources) {
        if (resources == null || resources.length == 0) {
            throw new IllegalArgumentException("At least one EngineResource is required");
        }

        this.agent = EngineLocks.currentAgent();
        this.reason = (reason == null || reason.trim().length() == 0)
                ? "test scope" : reason.trim();

        EngineResource[] ordered = resources.clone();
        Arrays.sort(ordered);

        try {
            for (EngineResource resource : ordered) {
                leases.add(EngineLocks.acquire(resource, LockMode.TEST,
                        agent, this.reason, EngineLocks.TEST_WAIT_MS));
            }
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    public String agent() {
        return agent;
    }

    public String reason() {
        return reason;
    }

    public List<EngineResource> resources() {
        List<EngineResource> result = new ArrayList<EngineResource>(leases.size());
        for (ResourceLease lease : leases) {
            result.add(lease.resource());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        for (int i = leases.size() - 1; i >= 0; i--) {
            ResourceLease lease = leases.get(i);
            try {
                lease.close();
            } catch (RuntimeException e) {
                Log.w(LOG_TAG, "Failed to release test lease " + lease + ": " + e);
            }
        }
        leases.clear();
    }
}
