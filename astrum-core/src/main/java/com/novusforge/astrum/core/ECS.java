package com.novusforge.astrum.core;

import java.util.*;

/**
 * Minimalist Entity-Component System (ECS) as requested in Formulas Part 1.
 * Focused on performance and cache-friendliness.
 */
public class ECS {
    private int nextEntityId = 0;
    private final Map<Class<?>, Map<Integer, Object>> components = new HashMap<>();

    public int createEntity() {
        return nextEntityId++;
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(int entityId, Class<T> componentClass) {
        Map<Integer, Object> entityMap = components.get(componentClass);
        if (entityMap == null) return null;
        return (T) entityMap.get(entityId);
    }

    public <T> void addComponent(int entityId, T component) {
        components.computeIfAbsent(component.getClass(), k -> new HashMap<>())
                  .put(entityId, component);
    }

    public void removeEntity(int entityId) {
        for (Map<Integer, Object> entityMap : components.values()) {
            entityMap.remove(entityId);
        }
    }

    public Set<Integer> getEntitiesWith(Class<?>... componentClasses) {
        if (componentClasses.length == 0) return Collections.emptySet();
        
        Set<Integer> entityIds = null;
        for (Class<?> clazz : componentClasses) {
            Map<Integer, Object> entityMap = components.get(clazz);
            if (entityMap == null) return Collections.emptySet();
            if (entityIds == null) {
                entityIds = new HashSet<>(entityMap.keySet());
            } else {
                entityIds.retainAll(entityMap.keySet());
            }
        }
        return entityIds == null ? Collections.emptySet() : entityIds;
    }
}
