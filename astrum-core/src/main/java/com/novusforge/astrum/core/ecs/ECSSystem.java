package com.novusforge.astrum.core.ecs;

/**
 * Base interface for ECS systems.
 * Systems contain logic that operates on entities with specific components.
 */
public interface ECSSystem {
    
    /**
     * Update the system - called every frame
     * @param manager The ECS manager
     * @param deltaTime Time since last frame in seconds
     */
    void update(ECSSystemManager manager, float deltaTime);
    
    /**
     * Get system name for debugging
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Check if system is enabled
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Called when system is initialized
     */
    default void init(ECSSystemManager manager) {}
    
    /**
     * Called when system is shut down
     */
    default void shutdown() {}
}
