package ru.framework2d.core.agents;

/**
 * Raised when a non-blocking acquisition finds the resource unavailable.
 * {@link #status()} carries the owner, mode, reason and waiters, so the caller
 * can decide whether a read is still allowed or whether it must wait.
 */
public class LockBusyException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final EngineResource resource;
    private final LockMode requestedMode;
    private final LockStatus status;

    public LockBusyException(String message, EngineResource resource,
                             LockMode requestedMode, LockStatus status) {
        super(message);
        this.resource = resource;
        this.requestedMode = requestedMode;
        this.status = status;
    }

    public EngineResource resource() {
        return resource;
    }

    public LockMode requestedMode() {
        return requestedMode;
    }

    public LockStatus status() {
        return status;
    }

    /** Human-readable explanation suitable for logs or agent reports. */
    public String describe() {
        return status == null ? getMessage() : status.describe();
    }
}
