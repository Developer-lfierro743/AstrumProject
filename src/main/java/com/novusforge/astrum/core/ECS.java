package com.novusforge.astrum.core;

import java.util.*;

/**
 * High-performance Data-Oriented ECS.
 * Implements cached queries and entity ID reuse for maximum optimization.
 */
public class ECS {
    private static final int MAX_ENTITIES = 100_000;
    
    private final BitSet[] entityMasks;
    private final Map<Class<?>, Object[]> componentArrays = new HashMap<>();
    private final Map<Class<?>, Integer> componentIds = new HashMap<>();
    private int nextComponentId = 0;
    
    // Entity ID reuse
    private int nextEntityId = 0;
    private final Deque<Integer> freeEntities = new ArrayDeque<>();
    
    // Cached queries: BitSet mask -> List of entities
    private final Map<BitSet, List<Integer>> queryCache = new HashMap<>();

    public ECS() {
        entityMasks = new BitSet[MAX_ENTITIES];
        for (int i = 0; i < MAX_ENTITIES; i++) {
            entityMasks[i] = new BitSet(64);
        }
    }

    public int createEntity() {
        if (!freeEntities.isEmpty()) {
            int id = freeEntities.pop();
            entityMasks[id].clear();
            return id;
        }
        if (nextEntityId >= MAX_ENTITIES) throw new RuntimeException("Max entities reached");
        return nextEntityId++;
    }

    public void removeEntity(int entityId) {
        if (entityId < 0 || entityId >= nextEntityId) return;
        
        // Remove from all cached queries before clearing mask
        removeFromCache(entityId, entityMasks[entityId]);
        
        entityMasks[entityId].clear();
        // Clear components to avoid memory leaks
        for (Object[] array : componentArrays.values()) {
            array[entityId] = null;
        }
        freeEntities.push(entityId);
    }

    @SuppressWarnings("unchecked")
    public <T> void addComponent(int entityId, T component) {
        Class<T> clazz = (Class<T>) component.getClass();
        int cId = getComponentId(clazz);
        
        Object[] array = componentArrays.computeIfAbsent(clazz, k -> new Object[MAX_ENTITIES]);
        
        // Update cache if mask changes
        BitSet oldMask = (BitSet) entityMasks[entityId].clone();
        array[entityId] = component;
        entityMasks[entityId].set(cId);
        
        updateCache(entityId, oldMask, entityMasks[entityId]);
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponent(int entityId, Class<T> clazz) {
        Object[] array = componentArrays.get(clazz);
        if (array == null) return null;
        return (T) array[entityId];
    }

    public boolean hasComponent(int entityId, Class<?> clazz) {
        Integer cId = componentIds.get(clazz);
        return cId != null && entityMasks[entityId].get(cId);
    }

    /**
     * Returns a cached list of entities matching the required components.
     * Fast O(1) lookup after initial cache warm-up.
     */
    public List<Integer> getEntities(Class<?>... clazzes) {
        BitSet queryMask = new BitSet(64);
        for (Class<?> clazz : clazzes) {
            queryMask.set(getComponentId(clazz));
        }
        
        return queryCache.computeIfAbsent(queryMask, mask -> {
            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < nextEntityId; i++) {
                if (containsAll(entityMasks[i], mask)) {
                    result.add(i);
                }
            }
            return result;
        });
    }

    private int getComponentId(Class<?> clazz) {
        return componentIds.computeIfAbsent(clazz, k -> nextComponentId++);
    }

    private void updateCache(int entityId, BitSet oldMask, BitSet newMask) {
        for (Map.Entry<BitSet, List<Integer>> entry : queryCache.entrySet()) {
            BitSet queryMask = entry.getKey();
            boolean wasMatch = containsAll(oldMask, queryMask);
            boolean isMatch = containsAll(newMask, queryMask);
            
            if (!wasMatch && isMatch) {
                entry.getValue().add(entityId);
            } else if (wasMatch && !isMatch) {
                entry.getValue().remove(Integer.valueOf(entityId));
            }
        }
    }

    private void removeFromCache(int entityId, BitSet mask) {
        for (Map.Entry<BitSet, List<Integer>> entry : queryCache.entrySet()) {
            if (containsAll(mask, entry.getKey())) {
                entry.getValue().remove(Integer.valueOf(entityId));
            }
        }
    }

    private boolean containsAll(BitSet entity, BitSet query) {
        for (int i = query.nextSetBit(0); i >= 0; i = query.nextSetBit(i + 1)) {
            if (!entity.get(i)) return false;
        }
        return true;
    }
}
