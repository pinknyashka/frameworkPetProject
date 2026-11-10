package ru.framework2d.core.agents;

/**
 * Thrown when an agent tries to acquire a resource that comes before the
 * resource it already holds in the canonical {@link EngineResource} order.
 * Failing fast is preferable to a later deadlock.
 */
public class LockOrderException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final EngineResource requested;
    private final EngineResource held;

    public LockOrderException(EngineResource requested, EngineResource held) {
        super("Lock order violation: cannot acquire " + requested + " while holding " + held
                + "; acquire multiple resources in EngineResource enum order");
        this.requested = requested;
        this.held = held;
    }

    public EngineResource requested() {
        return requested;
    }

    public EngineResource held() {
        return held;
    }
}
