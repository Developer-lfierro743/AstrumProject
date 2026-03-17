package com.novusforge.astrum.core.ecs;

import com.novusforge.astrum.api.ecs.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ECS System Manager - manages entities, components, and systems.
 * High-performance Entity Component System as per "The Formula".
 */
public class ECSSystemManager {
    
    private int nextEntityId = 0;
    private final Map<Integer, Map<Class<? extends Component>, Component>> entities = new HashMap<>();
    private final List<ECSSystem> systems = new ArrayList<>();
    private final Map<Class<? extends Component>, List<EntitySubscription>> subscriptions = new HashMap<>();
    
    public ECSSystemManager() {}
    
    /**
     * Create a new entity
     */
    public int createEntity() {
        int entityId = nextEntityId++;
        entities.put(entityId, new HashMap<>());
        return entityId;
    }
    
    /**
     * Remove an entity
     */
    public void removeEntity(int entityId) {
        entities.remove(entityId);
    }
    
    /**
     * Add component to entity
     */
    public <T extends Component> void addComponent(int entityId, T component) {
        Map<Class<? extends Component>, Component> entityComponents = entities.get(entityId);
        if (entityComponents == null) {
            throw new IllegalArgumentException("Entity does not exist: " + entityId);
        }
        entityComponents.put(component.getClass(), component);
        notifySubscriptions(entityId, component.getClass());
    }
    
    /**
     * Get component from entity
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(int entityId, Class<T> componentClass) {
        Map<Class<? extends Component>, Component> entityComponents = entities.get(entityId);
        if (entityComponents == null) return null;
        return (T) entityComponents.get(componentClass);
    }
    
    /**
     * Remove component from entity
     */
    public void removeComponent(int entityId, Class<? extends Component> componentClass) {
        Map<Class<? extends Component>, Component> entityComponents = entities.get(entityId);
        if (entityComponents != null) {
            entityComponents.remove(componentClass);
        }
    }
    
    /**
     * Check if entity has component
     */
    public boolean hasComponent(int entityId, Class<? extends Component> componentClass) {
        Map<Class<? extends Component>, Component> entityComponents = entities.get(entityId);
        return entityComponents != null && entityComponents.containsKey(componentClass);
    }
    
    /**
     * Register a system
     */
    public void registerSystem(ECSSystem system) {
        systems.add(system);
    }
    
    /**
     * Unregister a system
     */
    public void unregisterSystem(ECSSystem system) {
        systems.remove(system);
    }
    
    /**
     * Update all systems (call each frame)
     */
    public void update(float deltaTime) {
        for (ECSSystem system : systems) {
            system.update(this, deltaTime);
        }
    }
    
    /**
     * Get all entities with specific component
     */
    public List<Integer> getEntitiesWith(Class<? extends Component> componentClass) {
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Map<Class<? extends Component>, Component>> entry : entities.entrySet()) {
            if (entry.getValue().containsKey(componentClass)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
    
    /**
     * Subscribe to component changes
     */
    public void subscribe(Class<? extends Component> componentClass, Consumer<Integer> callback) {
        subscriptions.computeIfAbsent(componentClass, k -> new ArrayList<>())
            .add(new EntitySubscription(callback));
    }
    
    private void notifySubscriptions(int entityId, Class<? extends Component> componentClass) {
        List<EntitySubscription> subs = subscriptions.get(componentClass);
        if (subs != null) {
            for (EntitySubscription sub : subs) {
                sub.callback.accept(entityId);
            }
        }
    }
    
    private static class EntitySubscription {
        final Consumer<Integer> callback;
        
        EntitySubscription(Consumer<Integer> callback) {
            this.callback = callback;
        }
    }
}
