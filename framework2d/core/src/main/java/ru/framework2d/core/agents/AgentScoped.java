package ru.framework2d.core.agents;

/**
 * Implemented by handlers/engines that declare which engine domain they own.
 * Agents should acquire only the domain returned here unless their task
 * explicitly spans another domain (then list it in the task plan).
 */
public interface AgentScoped {

    /** Domain this handler/engine is responsible for. */
    EngineResource scopeResource();

    /** Stable diagnostic name, normally agent id plus handler path. */
    String scopeName();
}
