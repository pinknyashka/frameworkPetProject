package ru.framework2d.core.agents;

/**
 * Raised when a blocking acquisition exceeds its timeout. The embedded
 * {@link LockStatus} tells the agent who holds the resource and why.
 */
public class LockTimeoutException extends LockBusyException {

    private static final long serialVersionUID = 1L;

    private final long waitedMillis;

    public LockTimeoutException(String message, EngineResource resource,
                                LockMode requestedMode, LockStatus status,
                                long waitedMillis) {
        super(message, resource, requestedMode, status);
        this.waitedMillis = waitedMillis;
    }

    public long waitedMillis() {
        return waitedMillis;
    }
}
